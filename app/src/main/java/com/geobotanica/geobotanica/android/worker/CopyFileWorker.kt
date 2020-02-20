package com.geobotanica.geobotanica.android.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.network.KEY_DEST_PATH
import com.geobotanica.geobotanica.network.KEY_FILE_NAME
import com.geobotanica.geobotanica.network.KEY_SOURCE_PATH
import com.geobotanica.geobotanica.network.KEY_TITLE
import com.geobotanica.geobotanica.ui.NOTIFICATION_CHANNEL_ID_DOWNLOADS
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.coroutineScope
import java.io.File
import kotlin.system.measureTimeMillis


const val COPY_FILE_TAG = "copy_file"

class CopyFileWorker(val context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    private val notificationId = System.currentTimeMillis().toInt()

    override suspend fun doWork(): Result = coroutineScope {
        val data = extractInputData()
        createNotification(data)
        copyFile(data)
    }

    private fun extractInputData(): CopyFileInputData {
        return CopyFileInputData(
                inputData.getString(KEY_SOURCE_PATH) ?: throw IllegalArgumentException("CopyFileWorker: missing source path"),
                inputData.getString(KEY_DEST_PATH) ?: throw IllegalArgumentException("CopyFileWorker: missing dest path"),
                inputData.getString(KEY_FILE_NAME) ?: throw IllegalArgumentException("CopyFileWorker: missing file name"),
                inputData.getString(KEY_TITLE) ?: throw IllegalArgumentException("CopyFileWorker: missing title")
        )
    }

    private fun copyFile(data: CopyFileInputData): Result {
        return try {
            val time = measureTimeMillis {
                val sourceFile = File(data.sourcePath, data.filename)
                val destFile = File(data.destPath, data.filename)
                sourceFile.copyTo(destFile, true)
            }
            Lg.d("${data.filename}: Finished copying ($time ms)")
            Result.success(inputData)
        } catch(e: Exception) {
            e.printStackTrace()
            Lg.e("CopyFileWorker error: ${e.message}")
            Result.failure()
        }
    }

    private suspend fun createNotification(data: CopyFileInputData) = setForeground(createForegroundInfo(data))

    private fun createForegroundInfo(data: CopyFileInputData): ForegroundInfo { // For updating foreground service notification
        val cancelPendingIntent = WorkManager.getInstance(context)
                .createCancelPendingIntent(id)

        val title = "${context.getString(R.string.copying_file)}: ${data.title}"
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_DOWNLOADS)
                .setContentTitle(title)
                .setTicker(title) // Note: Only for accessibility services
                .setProgress(100, 0, true)
                .setSmallIcon(R.drawable.ic_file_download_24dp)
                .setOngoing(true)
                .setChannelId(NOTIFICATION_CHANNEL_ID_DOWNLOADS) // For Android O and later only
                .addAction( // Note: No icon on P, just text
                        android.R.drawable.ic_menu_close_clear_cancel,
                        context.getString(R.string.cancel),
                        cancelPendingIntent)
                .build()

        return ForegroundInfo(notificationId, notification)
    }

    private data class CopyFileInputData(
            val sourcePath: String,
            val destPath: String,
            val filename: String,
            val title: String
    )
}