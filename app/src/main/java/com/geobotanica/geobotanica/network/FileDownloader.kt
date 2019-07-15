package com.geobotanica.geobotanica.network

import android.app.DownloadManager
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.worker.DecompressionWorker
import com.geobotanica.geobotanica.android.worker.ONLINE_ASSET_INDEX_KEY
import com.geobotanica.geobotanica.data_taxa.TaxaDatabaseValidator
import com.geobotanica.geobotanica.network.online_map.OnlineMapEntry
import com.geobotanica.geobotanica.ui.MainActivity
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Handle click downloadAsset notification. Maybe use ACTION_VIEW_DOWNLOADS

@Singleton
class FileDownloader @Inject constructor (
        private val mainActivity: MainActivity,
        private val storageHelper: StorageHelper,
        private val networkValidator: NetworkValidator,
        private val downloadManager: DownloadManager,
        private val taxaDatabaseValidator: TaxaDatabaseValidator
) {

    private val assetDownloadIds = mutableMapOf<Long, OnlineAsset>()
    private val _assetDownloadComplete = MutableLiveData<OnlineAsset>()
    val assetDownloadComplete: LiveData<OnlineAsset> = _assetDownloadComplete

    private val mapDownloadIds = mutableMapOf<Long, OnlineMapEntry>()
    private val _mapDownloadComplete = MutableLiveData<OnlineMapEntry>()
    val mapDownloadComplete: LiveData<OnlineMapEntry> = _mapDownloadComplete

    init {
        mainActivity.downloadComplete.observeForever { onDownloadComplete(it) }
    }


    private fun onDownloadComplete(downloadId: Long)  {
        if (assetDownloadIds.containsKey(downloadId)) {
            assetDownloadIds.remove(downloadId)
            val onlineAsset = assetDownloadIds[downloadId]!!
            if (storageHelper.isAssetDownloaded(onlineAsset)) {
                Lg.i("Downloaded ${onlineAsset.fileNameGzip}")
                val decompressionWorkerRequest = OneTimeWorkRequestBuilder<DecompressionWorker>()
                        .setInputData(workDataOf(ONLINE_ASSET_INDEX_KEY to onlineAsset))
                        .build()
                val workManager = WorkManager.getInstance()
                workManager.getWorkInfoByIdLiveData(decompressionWorkerRequest.id)
                        .observe(mainActivity, decompressionWorkerObserver)
                workManager.enqueue(decompressionWorkerRequest)
            } else {
                Lg.e("Error downloading ${onlineAsset.description}")
            }
        } else if (mapDownloadIds.containsKey(downloadId)) {
            mapDownloadIds.remove(downloadId)
            val onlineMapEntry = mapDownloadIds[downloadId]
            _mapDownloadComplete.value = onlineMapEntry
        }
    }

    fun downloadAsset(onlineFile: OnlineAsset) {
        val file = File(storageHelper.getDownloadPath(), onlineFile.fileNameGzip)

        val request = DownloadManager.Request(Uri.parse(onlineFile.url))
                .setTitle(onlineFile.descriptionWithSize)
                .setDescription("Downloading")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setVisibleInDownloadsUi(false)

                // TODO: Add these options to a preferences page
                .setAllowedOverMetered(networkValidator.isNetworkMetered()) // Warning dialog handles metered network permission.
                .setAllowedOverRoaming(false) // True by default.

        Lg.i("Started asset download: ${onlineFile.description}")
        val downloadId = downloadManager.enqueue(request)
        assetDownloadIds[downloadId] = onlineFile
    }

    fun downloadMap(onlineMapEntry: OnlineMapEntry) {
        val file = File(storageHelper.getMapsPath(), onlineMapEntry.filename)

        val request = DownloadManager.Request(Uri.parse(onlineMapEntry.url))
                .setTitle(onlineMapEntry.printName)
                .setDescription("Downloading")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setVisibleInDownloadsUi(false)
                .setAllowedOverMetered(networkValidator.isNetworkMetered()) // Warning dialog handles metered network permission.
                .setAllowedOverRoaming(false) // True by default.

        Lg.i("Started map download: ${onlineMapEntry.printName}")
        val downloadId = downloadManager.enqueue(request)
        mapDownloadIds[downloadId] = onlineMapEntry
    }

    private val decompressionWorkerObserver = Observer<WorkInfo> { info ->
        if (info != null && info.state.isFinished) {
            val onlineAssetIndex = info.outputData.getInt(ONLINE_ASSET_INDEX_KEY, -1)
            val onlineAsset = onlineAssetList[onlineAssetIndex]
            _assetDownloadComplete.value = onlineAsset
            Lg.d("Decompressed: ${onlineAsset.description}")

            if (onlineAsset.fileName == "taxa.db") {
                GlobalScope.launch {
                    if (taxaDatabaseValidator.isPopulated())
                        Lg.d("isTaxaDbPopulated() = true")
                    else {
                        Lg.e("isTaxaDbPopulated() = false")
//                        Toast.makeText(applicationContext,
//                                getString(R.string.error_importing_plant_db),
//                                Toast.LENGTH_SHORT)
//                                .show()
                    }
                }
            }
        }
    }
}