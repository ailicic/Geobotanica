package com.geobotanica.geobotanica.android.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.network.*
import com.geobotanica.geobotanica.ui.NOTIFICATION_CHANNEL_ID_DOWNLOADS
import com.geobotanica.geobotanica.util.Lg
import okio.buffer
import okio.gzip
import okio.sink
import okio.source
import java.io.File
import kotlin.system.measureTimeMillis


const val DECOMPRESSION_TAG = "decompression"
private const val DECOMPRESSION_BUFFER_SIZE = 32_768L // b

class DecompressionWorker(val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val notificationId = System.currentTimeMillis().toInt()

    override suspend fun doWork(): Result {
        val data = extractInputData()
        val result = decompressFile(data)
        deleteCompressedFile(data)
        return result
    }

    private fun extractInputData(): DecompressionInputData {
        return DecompressionInputData(
                inputData.getString(KEY_DEST_PATH) ?: throw NoSuchElementException("DecompressionWorker: missing local path"),
                inputData.getString(KEY_FILE_NAME) ?: throw NoSuchElementException("DecompressionWorker: missing file name"),
                inputData.getLong(KEY_FILE_SIZE, 0L),
                inputData.getLong(KEY_DECOMPRESSED_FILE_SIZE, 0L),
                inputData.getString(KEY_TITLE) ?: throw NoSuchElementException("DecompressionWorker: missing title")
        )
    }

    private suspend fun decompressFile(data: DecompressionInputData): Result {
        if (! data.filename.endsWith(".gz")) {
            Lg.e("DecompressionWorker: File ${data.filename} does not end with .gz")
            deleteFiles(data)
            return Result.failure()
        }
        Lg.d("${data.filename}: Start decompressing")

        try {
            val time = measureTimeMillis {
                val gzipSourceFile = File(data.localPath, data.filename)
                val gzipSource = gzipSourceFile.source().gzip().buffer()

                val unGzipFile = File(data.localPath, data.filename.removeSuffix(".gz"))
                if (unGzipFile.exists())
                    unGzipFile.delete()
                val unGzipSink = unGzipFile.sink().buffer()

                var bytesRead = 0L
                var lastProgress = 0

                updateNotification(data, 0)

                while (!gzipSource.exhausted()) {
                    bytesRead += gzipSource.read(unGzipSink.buffer, DECOMPRESSION_BUFFER_SIZE)
                    unGzipSink.flush()
                    val progress = (bytesRead * 100L / data.decompressedFileSize).toInt()
                    if (progress - lastProgress >= 5) {
                        lastProgress = progress
                        updateNotification(data, progress)
                        Lg.v("${data.filename}: $progress% decompressed")
                    }
                }
                gzipSource.close()
                unGzipSink.close()
                gzipSourceFile.delete()
                if (bytesRead != data.decompressedFileSize) {
                    Lg.e("DecompressionWorker: ${data.filename} bytesRead mismatch (expected ${data.decompressedFileSize} b, but found $bytesRead b)")
                    deleteFiles(data)
                    return Result.failure()
                }
            }
            Lg.d("${data.filename}: Decompression complete ($time ms)")
            return Result.success(createOutputData())
        } catch (e: Exception) {
            e.printStackTrace()
            Lg.e("DecompressionWorker: $e")
            deleteFiles(data)
            return Result.failure()
        }
    }

    private fun deleteCompressedFile(data: DecompressionInputData) = File(data.localPath, data.filename).delete()

    private fun deleteFiles(data: DecompressionInputData) {
        File(data.localPath, data.filename).delete()
        File(data.localPath, data.filename.removeSuffix(".gz")).delete()
    }

    private suspend fun updateNotification(data: DecompressionInputData, progress: Int) {
        setForeground(createForegroundInfo(data, progress))
    }

    private fun createForegroundInfo(data: DecompressionInputData, progress: Int): ForegroundInfo { // For updating foreground service notification
        val title = "${context.getString(R.string.decompressing)}: ${data.title}"

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_DOWNLOADS)
                .setContentTitle(title)
                .setTicker(title) // Note: Only for accessibility services
                .setProgress(100, progress, false)
                .setContentText("$progress%")
                .setSmallIcon(R.drawable.ic_file_download_24dp)
                .setOngoing(true)
                .setChannelId(NOTIFICATION_CHANNEL_ID_DOWNLOADS) // For Android O and later only
                .build()

        return ForegroundInfo(notificationId, notification)
    }

    private fun createOutputData(): Data {
        return inputData.keyValueMap.toMutableMap().let { map ->
            map[KEY_FILE_NAME] = (map.getValue(KEY_FILE_NAME) as String).removeSuffix(".gz")
            workDataOf(*(map.toList().toTypedArray()))
        }
    }

    private data class DecompressionInputData(
            val localPath: String,
            val filename: String,
            val fileSize: Long,
            val decompressedFileSize: Long,
            val title: String
    )
}