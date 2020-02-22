package com.geobotanica.geobotanica.android.worker

import android.app.DownloadManager
import android.app.DownloadManager.Request.VISIBILITY_HIDDEN
import android.app.DownloadManager.STATUS_RUNNING
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.network.*
import com.geobotanica.geobotanica.ui.NOTIFICATION_CHANNEL_ID_DOWNLOADS
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.invokeOnCancelOrError
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.io.File
import kotlin.coroutines.CoroutineContext


const val DOWNLOAD_TAG = "download"

private const val DOWNLOAD_POLL_TIME = 1000L // ms
private const val DOWNLOAD_TIMEOUT = 10 * 60 * 1000 // 10 min

class DownloadWorker(val context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    private val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    private val notificationId = System.currentTimeMillis().toInt()

    override suspend fun doWork(): Result = coroutineScope {
        val data = extractInputData()
        val downloadId = startDownload(data)
        registerCancellationListener(coroutineContext, downloadId)
        monitorDownloadProgress(downloadId, data)
    }

    private fun extractInputData(): DownloadInputData {
        return DownloadInputData(
                inputData.getString(KEY_DOWNLOAD_URL) ?: throw IllegalArgumentException("DownloadWorker: missing download url"),
                inputData.getString(KEY_SOURCE_PATH) ?: throw IllegalArgumentException("DownloadWorker: missing source path"),
                inputData.getString(KEY_FILE_NAME) ?: throw IllegalArgumentException("DownloadWorker: missing file name"),
                inputData.getLong(KEY_FILE_SIZE, 0L),
                inputData.getString(KEY_TITLE) ?: throw IllegalArgumentException("DownloadWorker: missing title")
        )
    }

    private fun startDownload(data: DownloadInputData): Long {
        val file = File(data.sourcePath, data.filename).apply {
            if (exists() && isFile) delete()
        }
        val defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val isMeteredNetworkAllowed = defaultSharedPrefs.getBoolean(sharedPrefsAllowMeteredNetwork, false)
        val request = DownloadManager.Request(Uri.parse(data.downloadUrl)).run {
            setTitle(data.title)
            setDescription(context.getString(R.string.downloading))
            setDestinationUri(file.toUri())
            setNotificationVisibility(VISIBILITY_HIDDEN)

            @Suppress("DEPRECATION")
            setVisibleInDownloadsUi(false) // True by default. Ignored in Q+.

            setAllowedOverMetered(isMeteredNetworkAllowed)
            setAllowedOverRoaming(false) // True by default.
        }

        val downloadId = downloadManager.enqueue(request)
        Lg.i("${data.filename}: Download started (id = $downloadId)")
        return downloadId
    }

    private fun registerCancellationListener(coroutineContext: CoroutineContext, downloadId: Long) {
        coroutineContext[Job]?.invokeOnCancelOrError {
            Lg.i("DownloadWorker: Cancelling download with id = $downloadId")
            downloadManager.remove(downloadId)
        }
    }

    private suspend fun monitorDownloadProgress(downloadId: Long, data: DownloadInputData): Result {
        val startTime =  System.currentTimeMillis()

        var isStarted = false
        var isTimeout = false

        while (! isTimeout) {
            val currentTime = System.currentTimeMillis()

            getDownloadInfo(downloadId)?.run {
//                if (! isStarted && status == STATUS_RUNNING && totalBytes != -1L) {
                if (data.fileSize != 0L && ! isStarted && status == STATUS_RUNNING && totalBytes != -1L) { // TODO: Revert after map file sizes are known
                    isStarted = true
                    if (totalBytes != data.fileSize)
                        Lg.w("DownloadWorker: Wrong file size for $title (expected ${data.fileSize} b, found $totalBytes b)")
                }
                if (currentTime - lastModifiedTimestamp > DOWNLOAD_TIMEOUT) {
                    Lg.w("DownloadWorker: ${data.filename} timed out!")
                    downloadManager.remove(id)
                    isTimeout = true
                }
                if (bytes == totalBytes) {
                    val duration = (currentTime - startTime) / 1000
                    Lg.i("${data.filename}: Download complete ($duration s)")
                    return Result.success(inputData)
                }

                updateNotification(data, progress)
                Lg.v("${data.filename}: $progress% downloaded")
                delay(DOWNLOAD_POLL_TIME)
            }
        }
        return Result.failure()
    }

    // NOTE: Manually cancelled downloads appear to be non-referencable by their downloadId
    private fun getDownloadInfo(downloadId: Long): DownloadInfo? {
        return downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).run {
            if (moveToFirst()) {
                return DownloadInfo(
                        downloadId,
                        getString(getColumnIndex(DownloadManager.COLUMN_TITLE)),
                        getString(getColumnIndex(DownloadManager.COLUMN_DESCRIPTION)),
                        getString(getColumnIndex(DownloadManager.COLUMN_URI)),
                        getInt(getColumnIndex(DownloadManager.COLUMN_STATUS)),
                        getInt(getColumnIndex(DownloadManager.COLUMN_REASON)),
                        getLong(getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                        getLong(getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                        getLong(getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP))
                )
            } else {
                null
            }
        }
    }

    private suspend fun updateNotification(data: DownloadInputData, progress: Int) = setForeground(createForegroundInfo(data, progress))

    private fun createForegroundInfo(data: DownloadInputData, progress: Int): ForegroundInfo { // For updating foreground service notification
        val cancelPendingIntent = WorkManager.getInstance(context)
                .createCancelPendingIntent(id)

        val title = "${context.getString(R.string.downloading)}: ${data.title}"
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_DOWNLOADS)
                .setContentTitle(title)
                .setTicker(title) // Note: Only for accessibility services
                .setProgress(100, progress, false)
                .setContentText("$progress%")
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

    private data class DownloadInputData(
            val downloadUrl: String,
            val sourcePath: String,
            val filename: String,
//            val fileSize: Long,
            var fileSize: Long?, // TODO: Revert this after map file sizes are exactly known
            val title: String
    )
}