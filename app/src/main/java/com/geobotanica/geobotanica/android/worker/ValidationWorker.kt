package com.geobotanica.geobotanica.android.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.data_taxa.TaxaDatabase
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.network.KEY_FILE_NAME
import com.geobotanica.geobotanica.network.KEY_TITLE
import com.geobotanica.geobotanica.ui.NOTIFICATION_CHANNEL_ID_DOWNLOADS
import com.geobotanica.geobotanica.ui.login.onlineAssetList
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TODO: Consider injecting validators
// https://android.jlelse.eu/injecting-into-workers-android-workmanager-and-dagger-948193c17684
// https://proandroiddev.com/dagger-2-setup-with-workmanager-a-complete-step-by-step-guild-bb9f474bde37

const val VALIDATION_TAG = "validation"

class ValidationWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val notificationId = System.currentTimeMillis().toInt()

    override suspend fun doWork(): Result {
        val data = extractInputData()
        createNotification(data)
        return validateDownload(data)
    }

    private fun extractInputData(): ValidationInputData {
        return ValidationInputData(
                inputData.getString(KEY_FILE_NAME) ?: throw NoSuchElementException("ValidationWorker: missing file name"),
                inputData.getString(KEY_TITLE) ?: throw NoSuchElementException("ValidationWorker: missing title")
        )
    }

    private suspend fun createNotification(data: ValidationInputData) =
            setForeground(createForegroundInfo(data))

    private fun createForegroundInfo(data: ValidationInputData): ForegroundInfo {
        val title = "${context.getString(R.string.validating)}: ${data.title}"

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_DOWNLOADS)
                .setContentTitle(title)
                .setTicker(title) // Note: Only for accessibility services
                .setProgress(100, 0, true)
                .setSmallIcon(R.drawable.ic_file_download_24dp)
                .setOngoing(true)
                .setChannelId(NOTIFICATION_CHANNEL_ID_DOWNLOADS) // For Android O and later only
                .build()

        return ForegroundInfo(notificationId, notification)
    }

    private suspend fun validateDownload(data: ValidationInputData): Result { @Suppress("CascadeIf")
        return if (onlineAssetList.any { it.filenameUngzip == data.filename }) // TODO: Use assetRepo instead?
            validateAsset(data)
        else if (data.filename.endsWith(".map"))
            validateMap(data)
        else {
            Lg.e("ValidationWorker: ${data.filename} not recognized")
            Result.failure()
        }
    }

    private suspend fun validateAsset(data: ValidationInputData) = withContext(Dispatchers.IO) {
        val assetValidator = buildAssetValidator()
        return@withContext if (assetValidator.isValid(data.filename))
            Result.success(inputData)
        else
            Result.failure()
    }

    private fun buildAssetValidator(): AssetValidator {
        val storageHelper = StorageHelper(context)

        val db = GbDatabase.getInstance(context)
        val assetRepo = AssetRepo(db.assetDao())
        val mapRepo = MapRepo(db.mapDao(), db.mapFolderDao())

        val taxaDb = TaxaDatabase.getInstance(context)
        val taxonRepo = TaxonRepo(taxaDb.taxonDao(), taxaDb.tagDao(), taxaDb.typeDao())
        val vernacularRepo = VernacularRepo(taxaDb.vernacularDao(), taxaDb.tagDao(), taxaDb.typeDao())

        return AssetValidator(storageHelper, assetRepo, mapRepo, taxonRepo, vernacularRepo)
    }

    private suspend fun validateMap(data: ValidationInputData) = withContext(Dispatchers.IO) {
        val mapValidator = buildMapValidator()
        return@withContext if (mapValidator.isValid(data.filename)) {
            Result.success(inputData)
        } else {
            Result.failure()
        }
    }

    private fun buildMapValidator(): MapValidator {
        val storageHelper = StorageHelper(context)
        val db = GbDatabase.getInstance(context)
        val mapRepo = MapRepo(db.mapDao(), db.mapFolderDao())

        return MapValidator(storageHelper, mapRepo)
    }

    private data class ValidationInputData(
            val filename: String,
            val title: String
    )
}