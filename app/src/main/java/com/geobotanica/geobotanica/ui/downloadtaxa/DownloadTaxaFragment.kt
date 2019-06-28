package com.geobotanica.geobotanica.ui.downloadtaxa

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.downloadtaxa.DownloadTaxaFragment.UiState.*
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import kotlinx.android.synthetic.main.fragment_download_taxa.*
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.buffer
import okio.gzip
import okio.sink
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.system.measureTimeMillis


class DownloadTaxaFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<DownloadTaxaViewModel>
    @Inject lateinit var networkValidator: NetworkValidator
    private lateinit var viewModel: DownloadTaxaViewModel

    private val client = OkHttpClient()

    private var internalStorageFree: Long = 0

    private var job: Job? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            Lg.d("Fragment args: userId=$userId")
        }

        viewModel.databasesPath = context.filesDir.absolutePath.replace("/files", "/databases")
        viewModel.createDatabasesFolder()

        @SuppressLint("UsableSpace")
        internalStorageFree = File(context.filesDir.absolutePath).usableSpace

        mainScope.launch {
            if (viewModel.isTaxaDbDownloaded() && viewModel.isTaxaDbPopulated())
                navigateToNext()
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_download_taxa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
        bindClickListeners()
    }

    private fun initUi() {
        fileNameText.text = getString(
                R.string.download_file_name,
                getString(R.string.plant_name_database),
                DB_SIZE_GZIP / 1024 / 1024)

        internalStorageText.text = getString(
                R.string.internal_storage, internalStorageFree / 1024 / 1024)
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    private fun bindClickListeners() {
        downloadButton.setOnClickListener(::onClickDownload)
        cancelButton.setOnClickListener(::onClickCancel)
        trashButton.setOnClickListener(::onClickTrash)
        fab.setOnClickListener(::onClickFab)
    }

    // TODO: Move stuff to ViewModel where appropriate
    // TODO: Extract download class. Might need manager + client.

    @Suppress("UNUSED_PARAMETER")
    private fun onClickDownload(view: View?) {
        if (!isStorageAvailable()) {
            showSnackbar(R.string.not_enough_internal_storage, R.string.Inspect) {
                startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            }
            return
        }

        networkValidator.runIfValid { download() }
    }

    private fun isStorageAvailable(): Boolean = internalStorageFree > DB_SIZE_UNGZIP * 1.5

    private fun download() {
        updateUi(BEGIN_DOWNLOAD)
        Lg.i("DownloadTaxaFragment: Starting download...")

        job = mainScope.launch {
            var fileSink: BufferedSink? = null
            try {
                val responseBody = getResponseBody()
                val source = responseBody.source().gzip()
                if (responseBody.contentLength() != DB_SIZE_GZIP) {
                    Lg.e("DownloadTaxaFragment: File size error")
                    showToast("File size error")
                }

                fileSink = getFileSink()
                var bytesRead = 0L


                val time = measureTimeMillis {
                    withContext(Dispatchers.IO) {
                        while (bytesRead < DB_SIZE_UNGZIP) {
                            if (! isActive)
                                break
                            val result = source.read(fileSink.buffer(), 32768)
                            fileSink.flush() // WARNING: OOM Exception if excluded
                            bytesRead += if (result > 0) result else 0
                            progressBar.progress = bytesRead.toInt()

                        }
                    }
                }
                Lg.d("time = $time ms")

                Lg.i("DownloadTaxaFragment: Download complete")
                if (viewModel.isTaxaDbPopulated()) {
                    showToast("Database imported")
                    updateUi(DOWNLOAD_COMPLETE)
                } else {
                    showToast("Database import failed")
                    cancelDownload()
                    Lg.e("DownloadTaxaFragment: Database import failed")
                }
                    
            } catch (e: IOException) {
                Lg.i("IOException = $e")
                val str = e.toString()
                when {
                    str.startsWith("java.net.UnknownHostException") -> { // Internet on but can't resolve hostname
                        showToast(R.string.unknown_host)
                    }
                    str.startsWith("java.net.ConnectException") -> { // Internet on but host ip unreachable
                        showToast(R.string.host_unreachable)
                    }
                    str.startsWith("java.net.SocketException") -> { // Connection lost mid-way
                        showToast(R.string.connection_lost)
                    }
                    str.startsWith("java.net.SocketTimeoutException") -> { // Connection lost mid-way
                        showToast(R.string.connection_timed_out)
                    }
                    else -> throw e
                }
                cancelDownload()
            } finally {
                fileSink?.close()
            }
        }
    }

    private fun getFileSink(): BufferedSink {
        Lg.d("Dir = ${viewModel.databasesPath}")
        val file = File(viewModel.databasesPath, TAXA_DB_FILE)
        if (!file.exists())
            file.createNewFile()
        return file.sink().buffer()
    }

    private suspend fun getResponseBody(): ResponseBody {
        val request: Request = Request.Builder()
                .url(url)
                .build()
        val call: Call = client.newCall(request)
        val response = withContext(Dispatchers.IO) { call.execute() }
        return response.body()!!
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickCancel(view: View?) = cancelDownload()

    private fun cancelDownload() {
        job?.cancel()
        Lg.i("DownloadTaxaFragment: Download cancelled.")
        updateUi(CANCEL_DOWNLOAD)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickTrash(view: View?) {
        val file = File(viewModel.databasesPath, "gb.db")
        Lg.d("Deleting gb.db (Result = ${file.delete()}")
        updateUi(DELETE_DOWNLOAD)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickFab(view: View?) {
        navigateToNext()
    }

    private fun navigateToNext() {
        val navController = activity.findNavController(R.id.fragment)
        navController.popBackStack()
        navController.navigate(R.id.mapFragment, createBundle())
    }

    private fun createBundle(): Bundle =
        bundleOf(userIdKey to viewModel.userId)

    private fun updateUi(state: UiState) {
        when (state) {
            BEGIN_DOWNLOAD -> {
                downloadButton.isInvisible = true
                cancelButton.isVisible = true
                progressBar.isInvisible = false
                progressSpinner.isVisible = true
                progressBar.progress = 0
                progressBar.max = DB_SIZE_UNGZIP // TODO: Move this
            }
            CANCEL_DOWNLOAD -> {
                downloadButton.isVisible = true
                cancelButton.isVisible = false
                progressBar.isInvisible = true
                progressSpinner.isVisible = false
            }
            DOWNLOAD_COMPLETE -> {
                trashButton.isVisible = true
                cancelButton.isVisible = false
                progressBar.isInvisible = true
                progressSpinner.isVisible = false
                fab.isVisible = true
            }
            DELETE_DOWNLOAD -> {
                downloadButton.isVisible = true
                trashButton.isVisible = false
                fab.isVisible = false
            }
        }
    }

    private enum class UiState {
        BEGIN_DOWNLOAD, CANCEL_DOWNLOAD, DOWNLOAD_COMPLETE, DELETE_DOWNLOAD
    }
}


//        val storageState = getExternalStorageState() // Should be: Environment.MEDIA_MOUNTED
//        Lg.d("storageState = $storageState")