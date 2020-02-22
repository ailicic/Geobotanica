package com.geobotanica.geobotanica.android.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.KEY_DEST_PATH
import com.geobotanica.geobotanica.network.KEY_FILE_NAME
import com.geobotanica.geobotanica.network.KEY_TITLE
import com.geobotanica.geobotanica.ui.NOTIFICATION_CHANNEL_ID_DOWNLOADS
import com.geobotanica.geobotanica.ui.login.OnlineAssetId.MAP_FOLDER_LIST
import com.geobotanica.geobotanica.ui.login.OnlineAssetId.MAP_LIST
import com.geobotanica.geobotanica.util.Lg
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.buffer
import okio.source
import java.io.File
import kotlin.system.measureTimeMillis


// TODO: Consider injecting Moshi + MapRepo + AssetRepo
// https://android.jlelse.eu/injecting-into-workers-android-workmanager-and-dagger-948193c17684
// https://proandroiddev.com/dagger-2-setup-with-workmanager-a-complete-step-by-step-guild-bb9f474bde37

const val DESERIALIZATION_TAG = "deserialization"

class DeserializationWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val notificationId = System.currentTimeMillis().toInt()

    override suspend fun doWork(): Result {
        val data = extractInputData()
        createNotification(data)
        return deserializeFile(data)
    }

    private fun extractInputData(): DeserializationInputData {
        return DeserializationInputData(
                inputData.getString(KEY_DEST_PATH) ?: throw NoSuchElementException("DeserializationWorker: missing local path"),
                inputData.getString(KEY_FILE_NAME) ?: throw NoSuchElementException("DeserializationWorker: missing file name"),
                inputData.getString(KEY_TITLE) ?: throw NoSuchElementException("DeserializationWorker: missing title")
        )
    }

    private suspend fun deserializeFile(data: DeserializationInputData): Result {
        val file = File(data.localPath, data.filename)
        val moshi = Moshi.Builder().build()
        val db = GbDatabase.getInstance(context)
        val assetRepo = AssetRepo(db.assetDao())
        val assetList = assetRepo.getAll()

        var count = 0

        try {
            val time = measureTimeMillis {
                val source = file.source().buffer()
                val json = source.readUtf8()
                source.close()

                when (data.filename) {
                    assetList[MAP_LIST.ordinal].filenameUngzip -> {
                        val mapRepo = MapRepo(db.mapDao(), db.mapFolderDao())
                        val mapListType = Types.newParameterizedType(List::class.java, OnlineMap::class.java)
                        val adapter = moshi.adapter<List<OnlineMap>>(mapListType)
                        val mapList = adapter.fromJson(json) ?: throw IllegalStateException()

                        count = mapRepo.insert(mapList).size
                    }
                    assetList[MAP_FOLDER_LIST.ordinal].filenameUngzip -> {
                        val mapRepo = MapRepo(db.mapDao(), db.mapFolderDao())
                        val mapFolderListType = Types.newParameterizedType(List::class.java, OnlineMapFolder::class.java)
                        val adapter = moshi.adapter<List<OnlineMapFolder>>(mapFolderListType)
                        val mapFolderList = adapter.fromJson(json) ?: throw IllegalStateException()

                        count = mapRepo.insertFolders(mapFolderList).size
                    }
                    else -> throw (IllegalArgumentException("DeserializationWorker: Input file ${data.filename} not recognized"))
                }
            }
            file.delete()
            Lg.d("${data.filename}: Deserialization complete ($count items in $time ms)")
            return Result.success(inputData)
        } catch (e: Exception){
            e.printStackTrace()
            Lg.e("DeserializationWorker: $e")
            file.delete()
            return Result.failure()
        }
    }

    private suspend fun createNotification(data: DeserializationInputData) = setForeground(createForegroundInfo(data))

    private fun createForegroundInfo(data: DeserializationInputData): ForegroundInfo {
        val title = "${context.getString(R.string.importing)}: ${data.title}"

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

    private data class DeserializationInputData(
            val localPath: String,
            val filename: String,
            val title: String
    )
}