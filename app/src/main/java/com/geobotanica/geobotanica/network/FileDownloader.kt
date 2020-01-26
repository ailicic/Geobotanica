package com.geobotanica.geobotanica.network

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.net.Uri
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
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
        mainActivity.downloadComplete.observe(mainActivity, Observer { onDownloadComplete(it) })
    }

    suspend fun downloadAsset(asset: OnlineAsset) {
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
            if (! moveToFirst())
                return false
            val uri = getString(getColumnIndex(COLUMN_URI))
            close()
            uri.endsWith(".map")
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

    suspend fun verifyAssets() = withContext(Dispatchers.IO) {
        cancelIncompleteAssetDownloads()
        verifyDownloadedAssets()
    }

    private suspend fun cancelIncompleteAssetDownloads() = withContext(Dispatchers.IO) {
        assetRepo.getIncomplete().forEach { asset ->
            if (asset.isDownloading)
                downloadManager.remove(asset.downloadId)
            setAssetToNotDownloaded(asset)
            Lg.d("Reset ${asset.filenameGzip} incomplete download to NOT_DOWNLOADED")
        }
    }

    private suspend fun verifyDownloadedAssets() = withContext(Dispatchers.IO) {
        assetRepo.getDownloaded().forEach { asset ->
            when (asset.id) {
                OnlineAssetId.MAP_FOLDER_LIST.id -> {
                    if (mapRepo.getAllFolders().isEmpty()) { // TODO: Verify based on expected count instead
                        setAssetToNotDownloaded(asset)
                        Lg.d("Reset ${asset.filename} missing asset to NOT_DOWNLOADED")
                    }
                }
                OnlineAssetId.MAP_LIST.id -> {
                    if (mapRepo.getAll().isEmpty()) { // TODO: Verify based on expected count instead
                        setAssetToNotDownloaded(asset)
                        Lg.d("Reset ${asset.filename} missing asset to NOT_DOWNLOADED")
                    }
                }
                OnlineAssetId.WORLD_MAP.id -> {
                    if (! storageHelper.isAssetAvailable(asset)) {
                        setAssetToNotDownloaded(asset)
                        Lg.d("Reset ${asset.filename} missing asset to NOT_DOWNLOADED")
                    }
                }
                OnlineAssetId.PLANT_NAMES.id -> {
                    if (! taxaDatabaseValidator.isPopulated()) {
                        setAssetToNotDownloaded(asset)
                        Lg.d("Reset ${asset.filename} missing asset to NOT_DOWNLOADED")
                    }
                }
            }
        }
    }

    private suspend fun setAssetToNotDownloaded(asset: OnlineAsset) {
        asset.status = NOT_DOWNLOADED
        assetRepo.update(asset)
        File(storageHelper.getLocalPath(asset), asset.filenameGzip).delete()
        File(storageHelper.getLocalPath(asset), asset.filename).delete()
    }

    suspend fun verifyMaps() = withContext(Dispatchers.IO) {
//        updateStatusOfMapDownloads()
        verifyDownloadedMaps()
    }

    // TODO: Should be able to delete this if background service works out
//    private suspend fun updateStatusOfMapDownloads() = withContext(Dispatchers.IO) {
//        mapRepo.getDownloading().forEach { map ->
//            if (downloadExists(map.downloadId)) {
//                getDownloadInfo(map.downloadId)?.let { downloadInfo ->
//                    if (downloadInfo.status == STATUS_SUCCESSFUL && storageHelper.isMapAvailable(map)) {
//                        map.status = DOWNLOADED
//                        mapRepo.update(map)
//                        Lg.d("Updated ${map.filename} active download to DOWNLOADED")
//                    } else if (downloadInfo.progress < 1f) {
//                        downloadManager.remove(map.downloadId)
//                        map.status = NOT_DOWNLOADED
//                        mapRepo.update(map)
//                        Lg.d("Reset ${map.filename} low progress download to NOT_DOWNLOADED")
//                    }; Unit
//                }
//            } else {
//                map.status = NOT_DOWNLOADED
//                mapRepo.update(map)
//                Lg.d("Reset ${map.filename} missing active download to NOT_DOWNLOADED")
//            }
//        }
//    }

    private suspend fun verifyDownloadedMaps() = withContext(Dispatchers.IO) {
        mapRepo.getDownloaded().forEach { map ->
            if (! storageHelper.isMapAvailable(map)) {
                map.status = NOT_DOWNLOADED
                mapRepo.update(map)
                File(storageHelper.getMapsPath(), map.filename).delete()
                Lg.d("Reset ${map.filename} missing completed download to NOT_DOWNLOADED")
            }

        }
    }

    private suspend fun isAsset(downloadId: Long): Boolean {
        return downloadManager.query(Query().setFilterById(downloadId)).run {
            if (! moveToFirst())
                return false
            val isAsset = assetRepo.getAll().any {
                it.url == getString(this.getColumnIndex(COLUMN_URI))
            }
            close()
            isAsset
        }
    }

    private fun onDownloadComplete(downloadId: Long) {
        mainActivity.lifecycleScope.launch(Dispatchers.IO) {
            Lg.v("FileDownloader: onDownloadComplete() ${getDownloadInfo(downloadId)}")
            getDownloadInfo(downloadId)?.let { downloadInfo ->
                if (downloadInfo.status == STATUS_SUCCESSFUL) {
                    if (isAsset(downloadId)) {
                        assetRepo.getByDownloadId(downloadId)?.let { asset ->
                            Lg.i("Downloaded asset: ${asset.filenameGzip}")
                            if (storageHelper.isGzipAssetAvailable(asset))
                                decompressAsset(asset)
                            else
                                Lg.e("onDownloadComplete: Downloaded asset ${asset.filenameGzip} not available")
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
    }

    suspend fun decompressAsset(asset: OnlineAsset) {
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

    // NOTE: Manually cancelled downloads appear to be non-referencable by their downloadId
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
                mainActivity.lifecycleScope.launch(Dispatchers.IO) {
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


    private suspend fun deserializeMapFolderList(mapFoldersAsset: OnlineAsset) = withContext(Dispatchers.IO) {
        val mapFolderListFile = File(storageHelper.getLocalPath(mapFoldersAsset), mapFoldersAsset.filename)
        try {
            val time = measureTimeMillis {
                val source = mapFolderListFile.source().buffer()
                val mapFolderListJson = source.readUtf8()
                source.close()

                val mapFolderListType = Types.newParameterizedType(List::class.java, OnlineMapFolder::class.java)
                val adapter = moshi.adapter<List<OnlineMapFolder>>(mapFolderListType)
                val mapFolderList = adapter.fromJson(mapFolderListJson) ?: throw IllegalStateException()
                mapRepo.insertFolders(mapFolderList)
            }
            Lg.d("Deserialized asset: ${mapFoldersAsset.filename} ($time ms)")
            mapFolderListFile.delete()
        } catch (e: IOException){
            Lg.e("deserializeMapList(): $e")
            mapFolderListFile.delete()
        }
    }

    private suspend fun deserializeMapList(mapListAsset: OnlineAsset) = withContext(Dispatchers.IO) {
        Lg.d("Deserializing asset: ${mapListAsset.filename}")
        val mapListFile = File(storageHelper.getLocalPath(mapListAsset), mapListAsset.filename)
        try {
            val time = measureTimeMillis {
                val source = mapListFile.source().buffer()
                val mapsListJson = source.readUtf8()
                source.close()

                val mapListType = Types.newParameterizedType(List::class.java, OnlineMap::class.java)
                val adapter = moshi.adapter<List<OnlineMap>>(mapListType)
                val onlineMapList = adapter.fromJson(mapsListJson) ?: throw IllegalStateException()
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
) {
    val progress: Float
        get() = (bytes.toFloat() / totalBytes.toFloat()).absoluteValue
}