package com.geobotanica.geobotanica.android.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.network.remoteFileList
import com.geobotanica.geobotanica.util.Lg
import okio.buffer
import okio.gzip
import okio.sink
import okio.source
import java.io.File

const val REMOTE_FILE_KEY = "RemoteFileKey"

class DecompressionWorker(appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val remoteFileIndex = inputData.getInt(REMOTE_FILE_KEY, -1)
        return unzipDownloadedFile(remoteFileIndex)
    }

    private fun unzipDownloadedFile(remoteFileIndex: Int): Result {
        try {
            val remoteFile = remoteFileList[remoteFileIndex]
            Lg.d("Decompressing ${remoteFile.fileNameGzip}")
            val storageHelper = StorageHelper(applicationContext)

            val gzipSourceFile = File(storageHelper.getDownloadPath(), remoteFile.fileNameGzip)
            val gzipSource = gzipSourceFile.source().gzip().buffer()

            storageHelper.mkdirs(remoteFile)
            val unGzipSink = File(storageHelper.getLocalPath(remoteFile), remoteFile.fileName).sink().buffer()

            while (!gzipSource.exhausted()) {
                gzipSource.read(unGzipSink.buffer(), 32768)
                unGzipSink.flush()
            }
            gzipSource.close()
            unGzipSink.close()
            gzipSourceFile.delete()

            val output = workDataOf(REMOTE_FILE_KEY to remoteFileIndex)
            return Result.success(output)
        } catch (e: Exception) {
            e.printStackTrace()
            Lg.e("unzipDownloadedFile(): ERROR - $e")
            return Result.failure()
        }
    }
}