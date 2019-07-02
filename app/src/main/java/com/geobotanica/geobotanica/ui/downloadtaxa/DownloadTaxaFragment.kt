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
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus
import com.geobotanica.geobotanica.network.FileDownloader.Error.NO_STORAGE
import com.geobotanica.geobotanica.network.RemoteFile
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.downloadtaxa.DownloadTaxaFragment.UiState.*
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import kotlinx.android.synthetic.main.fragment_download_taxa.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import java.io.File
import javax.inject.Inject

// TODO: Move stuff to ViewModel where appropriate

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class DownloadTaxaFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<DownloadTaxaViewModel>
    private lateinit var viewModel: DownloadTaxaViewModel

    @Inject lateinit var fileDownloader: FileDownloader

    private var mainScope = CoroutineScope(Dispatchers.Main) + Job() // Need var to re-instantiate after cancellation

    private val remoteFile = RemoteFile( // TODO: Get from API
            "http://people.okanagan.bc.ca/ailicic/Markers/taxa.db.gz",
            "taxa.db",
            "databases",
            false,
            29_038_255,
            129_412_096
    )

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            Lg.d("Fragment args: userId=$userId")
        }

        viewModel.databasesPath = context.filesDir.absolutePath.replace("/files", "/databases")
        viewModel.createDatabasesFolder()

        mainScope.launch {
            if (fileDownloader.isFileDownloaded(remoteFile) && viewModel.isTaxaDbPopulated())
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

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }

    @SuppressLint("UsableSpace")
    private fun initUi() {
        fileNameText.text = getString(
                R.string.download_file_name,
                getString(R.string.plant_name_database),
                remoteFile.compressedSize / 1024 / 1024)
        internalStorageText.text = getString(R.string.internal_storage,
                File(context?.filesDir?.absolutePath).usableSpace / 1024 / 1024)
    }

    private fun bindClickListeners() {
        downloadButton.setOnClickListener(::onClickDownload)
        cancelButton.setOnClickListener(::onClickCancel)
        trashButton.setOnClickListener(::onClickTrash)
        fab.setOnClickListener(::onClickFab)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickDownload(view: View?) {
        mainScope.launch {
                fileDownloader.get(remoteFile, mainScope).consumeEach { status: DownloadStatus ->
                Lg.d("Download status = $status")

                status.error?.let {
                    if (status.error == NO_STORAGE) {
                        showSnackbar(R.string.not_enough_internal_storage, R.string.Inspect) {
                            startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
                        }
                    } else {
                        showToast(it.toString())
                        Lg.e("File download error: $it")
                        cancelDownload()
                    }
                    return@consumeEach
                }

                if (status.progress == 0f)
                    updateUi(BEGIN_DOWNLOAD)

                if (status.isComplete) {
                    if (viewModel.isTaxaDbPopulated()) {
                        Lg.i("DownloadTaxaFragment: Database imported")
                        showToast("Database imported")
                        updateUi(DOWNLOAD_COMPLETE)
                    } else {
                        showToast(getString(R.string.database_import_failed))
                        cancelDownload()
                        Lg.e("DownloadTaxaFragment: Database import failed")
                    }
                } else
                    progressBar.progress = status.progress.toInt()
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickCancel(view: View?) = cancelDownload()

    @ExperimentalCoroutinesApi
    private fun cancelDownload() {
        Lg.i("DownloadTaxaFragment: Download cancelled.")
        mainScope.cancel()
        mainScope += Job() // TODO: Required to launch another coroutine in this scope. Better approach?
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