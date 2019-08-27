package com.geobotanica.geobotanica.network

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.net.Uri
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.worker.DecompressionWorker
import com.geobotanica.geobotanica.android.worker.DecompressionWorker.Companion.ASSET_FILENAME
import com.geobotanica.geobotanica.android.worker.DecompressionWorker.Companion.ASSET_ID
import com.geobotanica.geobotanica.android.worker.DecompressionWorker.Companion.ASSET_LOCAL_PATH
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.data.entity.OnlineAssetId
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.data_taxa.TaxaDatabaseValidator
import com.geobotanica.geobotanica.ui.MainActivity
import com.geobotanica.geobotanica.util.Lg
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

// TODO: Seems like FileDownloader has too many responsibilities: decompression, deserialization, download status management
// -> Number and breadth of dependencies is concerning but encapsulation of implementation details is working well now.


@Singleton
class FileDownloader @Inject constructor (
        private val mainActivity: MainActivity, // TODO: Uncomfortable with this reference but need downloadComplete notifications
        private val storageHelper: StorageHelper,
        private val networkValidator: NetworkValidator,
        private val downloadManager: DownloadManager,
        private val taxaDatabaseValidator: TaxaDatabaseValidator,
        private val assetRepo: AssetRepo,
        private val mapRepo: MapRepo,
        private val moshi: Moshi
) {

    init {
        synchronizeDbDownloadStatuses()
        mainActivity.downloadComplete.observe(mainActivity, Observer { onDownloadComplete(it) })
    }

    suspend fun downloadAsset(asset: OnlineAsset) {
        if (getAssetFromExtStorage(asset)) {
            decompressAsset(asset)
            return
        }

        val file = File(storageHelper.getDownloadPath(), asset.filenameGzip)

        val request = Request(Uri.parse(asset.url))
                .setTitle(asset.printName)
                .setDescription("Downloading")
                .setNotificationVisibility(Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
//                .setVisibleInDownloadsUi(false) // True by default -> allows user to delete misbehaving downloads

                // TODO: Add these options to a preferences page
                .setAllowedOverMetered(networkValidator.isNetworkMetered()) // Warning dialog handles metered network permission.
                .setAllowedOverRoaming(false) // True by default.

        val downloadId = downloadManager.enqueue(request)
        asset.status = downloadId
        assetRepo.update(asset)
        Lg.i("Downloading asset: ${asset.filenameGzip}")
    }

    private fun getAssetFromExtStorage(asset: OnlineAsset): Boolean {
        Lg.d("getAssetFromExtStorage(): ${asset.filenameGzip}")
        val assetFileGzip = File(storageHelper.getExtStorageRootDir(), asset.filenameGzip)
        if (assetFileGzip.exists()) {
            Lg.d("Found asset on external storage: ${asset.filenameGzip}")
            assetFileGzip.copyTo(File(storageHelper.getDownloadPath(), asset.filenameGzip))
            return true
        } else
            return false
    }

    suspend fun downloadMap(onlineMap: OnlineMap) {
        val file = File(storageHelper.getMapsPath(), onlineMap.filename)

        val request = Request(Uri.parse(onlineMap.url))
                .setTitle(onlineMap.printName)
                .setDescription("Downloading")
                .setNotificationVisibility(Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
//                .setVisibleInDownloadsUi(false) // True by default -> allows user to delete misbehaving downloads

                .setAllowedOverMetered(networkValidator.isNetworkMetered()) // Warning dialog handles metered network permission.
                .setAllowedOverRoaming(false) // True by default.

        Lg.i("Downloading map: ${onlineMap.filename}")
        val downloadId = downloadManager.enqueue(request)
        onlineMap.status = downloadId
        mapRepo.update(onlineMap)
    }


    fun isMap(downloadId: Long): Boolean {
        return downloadManager.query(Query().setFilterById(downloadId)).run {
            moveToFirst()
            val uri = getString(getColumnIndex(COLUMN_URI))
            close()
            uri.endsWith(".map")
        }
    }

    private suspend fun isAsset(downloadId: Long): Boolean {
        return downloadManager.query(Query().setFilterById(downloadId)).run {
            moveToFirst()
            val isAsset = assetRepo.getAll().any {
                it.url == getString(this.getColumnIndex(COLUMN_URI))
            }
            close()
            isAsset
        }
    }


    fun filenameFrom(downloadId: Long): String {
        return downloadManager.query(Query().setFilterById(downloadId)).run {
            moveToFirst()
            val uri = getString(getColumnIndex(COLUMN_URI))
            close()
            uri.substringAfterLast('/')
        }
    }

//    suspend fun removeQueuedDownloads() {
//        val query = Query().setFilterByStatus(STATUS_PENDING or STATUS_PAUSED or STATUS_RUNNING)
//        val cursor = downloadManager.query(query)
//        while (cursor.moveToNext()) {
//            val downloadId = cursor.getLong(cursor.getColumnIndex(COLUMN_ID))
//            val uri = cursor.getString(cursor.getColumnIndex(COLUMN_URI))
//            val status = cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS))
//            val title = cursor.getString(cursor.getColumnIndex(COLUMN_TITLE))
//            val bytes = cursor.getLong(cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR))
//            val totalBytes = cursor.getLong(cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES))
//            val progress = bytes.toFloat() / totalBytes.toFloat()
//
//            if (status == STATUS_RUNNING || status == STATUS_PAUSED) {
//                if (bytes == 0L ) {
//                    Lg.d("Removed zero progress download: $title")
//                    if (uri.endsWith(".map")) {
//                        val map = mapRepo.getByDownloadId(downloadId)
//                        map?.let {
//                            it.status = NOT_DOWNLOADED
//                            mapRepo.update(it)
//                        }
//                        Lg.e("FileDownloader: removeQueuedDownloads() -> ${map.filename} download not in OnlineMap database table")
//                    } else {
//                        val asset
//                    }
//                    downloadManager.remove(downloadId)
//
//                } else {
//                    Lg.d("Observed $progress% complete download: $title")
//                }
//            } else {
//                Lg.d("Removed pending download: $title")
//                downloadManager.remove(downloadId)
//            }
//            Lg.d("Removed download: $title (id = $downloadId, status = $status, bytes = $bytes B)")
//            downloadManager.remove(downloadId)
//        }
//        cursor.close()
//    }

    // TODO: Handle downloaded files that are manually deleted
    // TODO: Implementation is messy. But it catches some common incorrect db states.
    private fun synchronizeDbDownloadStatuses() { // Catch download/decompression completed while app not in foreground.
        Lg.d("FileDownloader: synchronizeDbDownloadStatuses()")
        GlobalScope.launch(Dispatchers.IO) {
            assetRepo.getDownloading().forEach { asset ->
                Lg.d("synchronizeDbDownloadStatuses(): Processing downloading asset $asset")
                if (downloadExists(asset.status)) {
                    if (isDownloadSuccessful(asset.status)) {
                        asset.status = DECOMPRESSING
                        assetRepo.update(asset)
                        Lg.d("Updated ${asset.filenameGzip} download status to decompressing")
                    }
                } else {
                    asset.status = NOT_DOWNLOADED
                    assetRepo.update(asset)
                    Lg.d("Reset ${asset.filenameGzip} download to NOT_DOWNLOADED")
                }
            }
            assetRepo.getDecompressing().forEach { asset ->
                if (storageHelper.isAssetAvailable(asset)) {
                    asset.status = DOWNLOADED
                    assetRepo.update(asset)
                    Lg.d("Updated ${asset.filenameGzip} download status to downloaded")
                } else
                    registerDecompressionObserver(asset.filenameGzip)
            }
            mapRepo.getDownloading().forEach { map ->
                Lg.d("synchronizeDbDownloadStatuses(): Processing downloading map $map")
                if (downloadExists(map.status)) {
                    if (isDownloadSuccessful(map.status)) {
                        map.status = DOWNLOADED
                        mapRepo.update(map)
                        Lg.d("Updated ${map.filename} download status to downloaded")
                    }
                } else {
                    map.status = NOT_DOWNLOADED
                    mapRepo.update(map)
                    Lg.d("Reset ${map.filename} download to NOT_DOWNLOADED")
                }
            }
        }
    }

    // NOTE: Manually cancelled downloads appear to be non-referencable by their downloadId
    private fun downloadExists(downloadId: Long): Boolean {
        return downloadManager.query(Query().setFilterById(downloadId)).run {
            val exists = moveToFirst()
            close()
            exists
        }
    }

    private fun isDownloadSuccessful(downloadId: Long): Boolean {
        return downloadManager.query(Query().setFilterById(downloadId)).run {
            moveToFirst()
            val status = getInt(getColumnIndex(COLUMN_STATUS))
            close()
            status == STATUS_SUCCESSFUL
        }
    }

    private fun onDownloadComplete(downloadId: Long) {
        Lg.v("FileDownloader: onDownloadComplete() ${getDownloadInfo(downloadId)}")
        if (! downloadExists(downloadId)) // Catches manually cancelled download
            synchronizeDbDownloadStatuses()
        else if (isDownloadSuccessful(downloadId)) {
            GlobalScope.launch(Dispatchers.IO) {
                if (isAsset(downloadId)) {
                    assetRepo.getByDownloadId(downloadId)?.let { asset ->
                        decompressAsset(asset)
                    }

                }
                if (isMap(downloadId)) {
                    mapRepo.getByDownloadId(downloadId)?.let { map ->
                        Lg.i("Downloaded map: ${map.filename}")
                        map.status = DOWNLOADED
                        mapRepo.update(map)
                        return@launch
                    }
                }
            }
        }
    }

    private suspend fun decompressAsset(asset: OnlineAsset) {
        Lg.i("Downloaded asset: ${asset.filenameGzip}")
        val decompressionWorkerRequest = OneTimeWorkRequestBuilder<DecompressionWorker>()
                .addTag(asset.filenameGzip)
                .setInputData(workDataOf(
                        ASSET_ID to asset.id,
                        ASSET_LOCAL_PATH to storageHelper.getLocalPath(asset),
                        ASSET_FILENAME to asset.filename))
                .build()
        val workManager = WorkManager.getInstance(mainActivity)
        registerDecompressionObserver(asset.filenameGzip)
        storageHelper.mkdirs(asset)
        asset.status = DECOMPRESSING
        assetRepo.update(asset)
        workManager.enqueue(decompressionWorkerRequest)
        return
    }

    private fun getDownloadInfo(downloadId: Long): DownloadInfo? {
        return downloadManager.query(Query().setFilterById(downloadId)).run {
            if (moveToFirst()) {
                return DownloadInfo(
                        downloadId,
                        getString(getColumnIndex(COLUMN_TITLE)),
                        getString(getColumnIndex(COLUMN_DESCRIPTION)),
                        getString(getColumnIndex(COLUMN_URI)),
                        getInt(getColumnIndex(COLUMN_STATUS)),
                        getInt(getColumnIndex(COLUMN_REASON)),
                        getLong(getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                        getLong(getColumnIndex(COLUMN_TOTAL_SIZE_BYTES)),
                        getLong(getColumnIndex(COLUMN_LAST_MODIFIED_TIMESTAMP))
                )
            } else {
                null
            }
        }

    }


    private suspend fun registerDecompressionObserver(workRequestTag: String) {
        withContext(Dispatchers.Main) {
            val workManager = WorkManager.getInstance(mainActivity)
            workManager.getWorkInfosByTagLiveData(workRequestTag)
                    .observe(mainActivity, decompressionWorkerObserver)
        }
    }

    private val decompressionWorkerObserver = Observer<MutableList<WorkInfo>> { infoList ->
        infoList.forEach {  info ->
            if (info.state.isFinished) {
                GlobalScope.launch(Dispatchers.IO) {
                    val assetId = info.outputData.getLong(ASSET_ID, -1)
                    val asset = assetRepo.get(assetId)

                    when (assetId) {
                        OnlineAssetId.PLANT_NAMES.id -> {
                            Lg.d("isTaxaDbPopulated() = ${taxaDatabaseValidator.isPopulated()}")
                        }
                        OnlineAssetId.MAP_FOLDER_LIST.id -> deserializeMapFolderList(asset)
                        OnlineAssetId.MAP_LIST.id -> deserializeMapList(asset)
                    }

                    asset.status = DOWNLOADED
                    assetRepo.update(asset)
                }
            }
        }
    }


    private suspend fun deserializeMapFolderList(mapFoldersAsset: OnlineAsset) {
        val mapFolderListFile = File(storageHelper.getLocalPath(mapFoldersAsset), mapFoldersAsset.filename)
        try {
            val time = measureTimeMillis {
                val source = mapFolderListFile.source().buffer()
                val mapFolderListJson = source.readUtf8()
                source.close()

                val mapFolderListType = Types.newParameterizedType(List::class.java, OnlineMapFolder::class.java)
                val adapter = moshi.adapter<List<OnlineMapFolder>>(mapFolderListType)
                val mapFolderList = adapter.fromJson(mapFolderListJson)!!
                mapRepo.insertFolders(mapFolderList)
            }
            Lg.d("Deserialized asset: ${mapFoldersAsset.filename} ($time ms)")
            mapFolderListFile.delete()
        } catch (e: IOException){
            Lg.e("deserializeMapList(): $e")
            mapFolderListFile.delete()
        }
    }

    private suspend fun deserializeMapList(mapListAsset: OnlineAsset) {
        Lg.d("Deserializing asset: ${mapListAsset.filename}")
        val mapListFile = File(storageHelper.getLocalPath(mapListAsset), mapListAsset.filename)
        try {
            val time = measureTimeMillis {
                val source = mapListFile.source().buffer()
                val mapsListJson = source.readUtf8()
                source.close()

                val mapListType = Types.newParameterizedType(List::class.java, OnlineMap::class.java)
                val adapter = moshi.adapter<List<OnlineMap>>(mapListType)
                val onlineMapList = adapter.fromJson(mapsListJson)!!
                mapRepo.insert(onlineMapList)
            }
            Lg.d("Deserialized asset: ${mapListAsset.filename} ($time ms)")
            mapListFile.delete()
        } catch (e: IOException){
            Lg.e("deserializeMapList(): $e")
            mapListFile.delete()
        }
    }

    fun cancelDownload(downloadId: Long): Int {
        return downloadManager.remove(downloadId)
    }

    companion object DownloadStatus {
        const val NOT_DOWNLOADED = 0L
        // DOWNLOADING > 0  ( = downloadId)
        const val DECOMPRESSING = -1L
        const val DOWNLOADED = -2L
    }
}

data class DownloadInfo(
        val id: Long,
        val title: String,
        val description: String,
        val uri: String,
        val status: Int,
        val reason: Int,
        val bytes: Long,
        val totalBytes: Long,
        val lastModifiedTimestamp: Long
)