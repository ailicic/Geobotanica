package com.geobotanica.geobotanica.ui.downloadmaps

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import kotlinx.android.synthetic.main.fragment_download_maps.*
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

// TODO: Move stuff to ViewModel where appropriate

private const val WORLD_MAP_URI = "http://people.okanagan.bc.ca/ailicic/Maps/world.map.gz" // TODO: Get from API
private const val BC_MAP_URI = "http://people.okanagan.bc.ca/ailicic/Maps/british-columbia.map.gz" // TODO: Get from API

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class DownloadMapsFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<DownloadMapViewModel>
    private lateinit var viewModel: DownloadMapViewModel

//    @Inject lateinit var fileDownloader: FileDownloader

    private var mainScope = CoroutineScope(Dispatchers.Main) + Job() // Need var to re-instantiate after cancellation

    private var downloadId: Long = 0L

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            Lg.d("Fragment args: userId=$userId")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_download_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
        bindClickListeners()
        registerBroadcastReceiver()
    }

    private fun registerBroadcastReceiver() {
        activity.registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.unregisterReceiver(onDownloadComplete)
        mainScope.cancel()
    }

    @SuppressLint("UsableSpace")
    private fun initUi() {

    }

    private fun bindClickListeners() {
//        downloadButton.setOnClickListener(::onClickDownload)
//        fab.setOnClickListener(::onClickFab)
        downloadButton.setOnClickListener(::onClickDownload)
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            if (id == downloadId) {
                showToast("Download complete")
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickDownload(view: View?) {
        val file = File(appContext.getExternalFilesDir(null)?.absolutePath, "british-columbia.map.gz")

//        val request = DownloadManager.Request(Uri.parse(WORLD_MAP_URI))
        val request = DownloadManager.Request(Uri.parse(BC_MAP_URI))
                .setTitle("British Columbia map")
                .setDescription("Downloading")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setAllowedOverMetered(true) // True by default
                .setAllowedOverRoaming(false) // True by default

        val downloadManager = activity.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
    }

//    @Suppress("UNUSED_PARAMETER")
//    private fun onClickCancel(view: View?) = cancelDownload()

    @ExperimentalCoroutinesApi
//    private fun cancelDownload() {
//        Lg.i("DownloadTaxaFragment: Download cancelled.")
//        mainScope.cancel()
//        mainScope += Job() // TODO: Required to launch another coroutine in this scope. Better approach?
//        updateUi(CANCEL_DOWNLOAD)
//    }

//    @Suppress("UNUSED_PARAMETER")
//    private fun onClickTrash(view: View?) {
//        val file = File(viewModel.databasesPath, "gb.db")
//        Lg.d("Deleting gb.db (Result = ${file.delete()}")
//        updateUi(DELETE_DOWNLOAD)
//    }

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

}