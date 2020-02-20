package com.geobotanica.geobotanica.android.file

import androidx.lifecycle.LiveData
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.geobotanica.geobotanica.android.worker.*
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.network.*
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.addTags
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileImporter @Inject constructor(
        private val storageHelper: StorageHelper,
        private val workManager: WorkManager
) {

    fun importFromStorage(asset: OnlineAsset): LiveData<List<WorkInfo>> {
        Lg.d("FileImporter: Importing ${asset.filename} from storage")

        val inputData = workDataOf(
                KEY_SOURCE_PATH to storageHelper.getExtStorageRootPath(),
                KEY_DEST_PATH to storageHelper.getLocalPath(asset),
                KEY_FILE_NAME to asset.filename,
                KEY_FILE_SIZE to asset.fileSize,
                KEY_DECOMPRESSED_FILE_SIZE to asset.decompressedSize,
                KEY_TITLE to asset.description,
                KEY_ITEM_COUNT to asset.itemCount
        )

        val copyFileWorker = OneTimeWorkRequestBuilder<CopyFileWorker>()
                .addTags(COPY_FILE_TAG, asset.filename)
                .setInputData(inputData)
                .build()
        var work = workManager.beginWith(copyFileWorker)

        val decompressionWorker = OneTimeWorkRequestBuilder<DecompressionWorker>()
                .addTags(DECOMPRESSION_TAG, asset.filename)
                .build()
        work = work.then(decompressionWorker)


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

    fun importFromStorage(map: OnlineMap) {

    }
}