package com.geobotanica.geobotanica.network

import androidx.lifecycle.LiveData
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.worker.*
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.util.addTags
import com.geobotanica.geobotanica.util.isRunning
import javax.inject.Inject
import javax.inject.Singleton


const val KEY_DOWNLOAD_URL = "KEY_DOWNLOAD_URL"
const val KEY_SOURCE_PATH = "KEY_SOURCE_PATH"
const val KEY_DEST_PATH = "KEY_DEST_PATH"
const val KEY_FILE_NAME = "KEY_FILE_NAME"
const val KEY_FILE_SIZE = "KEY_FILE_SIZE"
const val KEY_TITLE = "KEY_TITLE"
const val KEY_DECOMPRESSED_FILE_SIZE = "KEY_DECOMPRESSED_FILE_SIZE" // Optional
const val KEY_ITEM_COUNT = "KEY_ITEM_COUNT" // Optional. Used if JSON deserialized for db import.

@Singleton
class FileDownloader @Inject constructor (
        private val storageHelper: StorageHelper,
        private val workManager: WorkManager
) {

    fun download(asset: OnlineAsset): LiveData<List<WorkInfo>> {

        val inputData = workDataOf(
                KEY_DOWNLOAD_URL to asset.url,
                KEY_SOURCE_PATH to storageHelper.getDownloadPath(),
                KEY_DEST_PATH to storageHelper.getLocalPath(asset),
                KEY_FILE_NAME to asset.filename,
                KEY_FILE_SIZE to asset.fileSize,
                KEY_DECOMPRESSED_FILE_SIZE to asset.decompressedSize,
                KEY_TITLE to asset.description,
                KEY_ITEM_COUNT to asset.itemCount
        )

        val downloadWorker = OneTimeWorkRequestBuilder<DownloadWorker>()
                .addTags(DOWNLOAD_TAG, asset.filename)
                .setInputData(inputData)
                .build()

        var work = workManager.beginWith(downloadWorker)

        val moveFileWorker = OneTimeWorkRequestBuilder<MoveFileWorker>()
                .addTags(MOVE_FILE_TAG, asset.filename)
                .build()
        work = work.then(moveFileWorker)

        if (asset.shouldDecompress) {
            val decompressionWorker = OneTimeWorkRequestBuilder<DecompressionWorker>()
                    .addTags(DECOMPRESSION_TAG, asset.filename)
                    .build()
            work = work.then(decompressionWorker)
        }
        if (asset.shouldDeserialize) {
            val deserializationWorker = OneTimeWorkRequestBuilder<DeserializationWorker>()
                    .addTags(DESERIALIZATION_TAG, asset.filename)
                    .build()
            work = work.then(deserializationWorker)
        }

        val validationWorker = OneTimeWorkRequestBuilder<ValidationWorker>()
                .addTags(VALIDATION_TAG, asset.filename)
                .build()
        work = work.then(validationWorker)

        work.enqueue()
        return work.workInfosLiveData
    }

    fun isDownloading(asset: OnlineAsset): Boolean = workManager.getWorkInfosByTag(asset.filename).get().isRunning

    fun isDownloading(map: OnlineMap): Boolean = workManager.getWorkInfosByTag(map.filename).get().isRunning

    fun download(map: OnlineMap): LiveData<List<WorkInfo>> {

        val inputData = workDataOf(
                KEY_DOWNLOAD_URL to map.url,
                KEY_SOURCE_PATH to storageHelper.getMapsPath(),
                KEY_FILE_NAME to map.filename,
//                KEY_FILE_SIZE to map.fileSize, // TODO: Include after map file sizes are known
                KEY_TITLE to map.printName
        )

        val downloadWorker = OneTimeWorkRequestBuilder<DownloadWorker>()
                .addTags(DOWNLOAD_TAG, map.filename)
                .setInputData(inputData)
                .build()

        val validationWorker = OneTimeWorkRequestBuilder<ValidationWorker>()
                .addTags(VALIDATION_TAG, map.filename)
                .build()

        val work = workManager.beginWith(downloadWorker)
                .then(validationWorker)
        work.enqueue()
        return work.workInfosLiveData
    }

    fun cancelDownloadWork(asset: OnlineAsset) = workManager.cancelAllWorkByTag(asset.filename)

    fun cancelDownloadWork(map: OnlineMap) = workManager.cancelAllWorkByTag(map.filename)
}

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED
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
    val progress: Int
        get() = (bytes * 100 / totalBytes).toInt()
}