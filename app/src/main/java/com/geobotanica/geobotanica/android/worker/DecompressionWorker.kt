package com.geobotanica.geobotanica.android.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.geobotanica.geobotanica.util.Lg
import okio.buffer
import okio.gzip
import okio.sink
import okio.source
import java.io.File
import kotlin.system.measureTimeMillis


class DecompressionWorker(val appContext: Context, workerParams: WorkerParameters)
    : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val assetId = inputData.getLong(ASSET_ID, -1)
        val assetLocalPath = inputData.getString(ASSET_LOCAL_PATH)
        val assetFilename = inputData.getString(ASSET_FILENAME)
        val assetFilenameGzip = "$assetFilename.gz"
        val assetDownloadPath = appContext.getExternalFilesDir(null)?.absolutePath

        try {
//            Lg.d("Decompressing $assetFilenameGzip (assetId = $assetId, assetFilenameGzip = $assetFilenameGzip, assetLocalPath = $assetLocalPath)")
            Lg.d("Decompressing asset: $assetFilenameGzip")

            val time = measureTimeMillis {
                val gzipSourceFile = File(assetDownloadPath, assetFilenameGzip)
                val gzipSource = gzipSourceFile.source().gzip().buffer()

                val unGzipFile = File(assetLocalPath, assetFilename)
                if (unGzipFile.exists())
                    unGzipFile.delete()
                val unGzipSink = unGzipFile.sink().buffer()

                while (!gzipSource.exhausted()) {
                    gzipSource.read(unGzipSink.buffer, 32768)
                    unGzipSink.flush()
                }
                gzipSource.close()
                unGzipSink.close()
                gzipSourceFile.delete()
            }
            Lg.d("Decompressed asset: $assetFilenameGzip ($time ms)")
            val output = workDataOf(ASSET_ID to assetId)
            return Result.success(output)
        } catch (e: Exception) {
            e.printStackTrace()
            Lg.e("DecompressionWorker: $e")
            return Result.failure()
        }
    }

    companion object {
        val ASSET_ID = "AssetIdKey"
        val ASSET_FILENAME = "AssetFilenameKey"
        val ASSET_LOCAL_PATH = "AssetLocalPathKey"
    }
}