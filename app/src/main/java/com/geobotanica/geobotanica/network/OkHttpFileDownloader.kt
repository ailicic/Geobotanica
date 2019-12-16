package com.geobotanica.geobotanica.network

import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.IllegalStateException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class OkHttpFileDownloader@Inject constructor(
        private val okHttpClient: OkHttpClient
) {
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getJson(url: String): String {
        try {
            val request: Request = Request.Builder()
                    .url(url)
                    .build()
            val call: Call = okHttpClient.newCall(request)
            val response = withContext(Dispatchers.IO) { call.execute() }
            return response.body()?.string() ?: throw IllegalStateException()
        } catch (e: IOException) {
            Lg.e("IOException = $e")
            throw e
        }
    }
}

//    @Suppress("RemoveExplicitTypeArguments")
//    suspend fun get(onlineFile: OnlineFile, scope: CoroutineScope) = scope.produce<DownloadStatus>(coroutineContext) {
//        if (!storageHelper.isStorageAvailable(onlineFile)) {
//            send(DownloadStatus(error = NO_STORAGE))
//            return@produce
//        }
//        if (networkValidator.getStatus())
//            download(onlineFile, channel)
//    }
//
//    private suspend fun download(onlineFile: OnlineFile, channel: SendChannel<DownloadStatus>) {
//        Lg.i("FileDownloader: Starting download...")
//
//        var fileSink: BufferedSink? = null
//        try {
//            val responseBody = getResponseBody(onlineFile)
//            val source = responseBody.source().gzip()
//            if (responseBody.contentLength() != onlineFile.compressedSize) {
//                Lg.e("FileDownloader: File size error on $onlineFile")
//                channel.send(DownloadStatus(error = FILE_SIZE_MISMATCH))
//            }
//
//            fileSink = getFileSink(onlineFile)
//            var bytesRead = 0L
//
//
//            channel.send(DownloadStatus(0f))
//            val time = measureTimeMillis {
//                withContext(Dispatchers.IO) {
//                    var lastUpdate = System.currentTimeMillis()
//                    //                            while (bytesRead < DB_SIZE_UNGZIP) {
//                    while (bytesRead < onlineFile.decompressedSize) {
//                        if (! isActive) {
//                            break
//                        }
//                        val result = source.read(fileSink.buffer, 32768)
//                        fileSink.flush() // WARNING: OOM Exception if excluded
//                        bytesRead += if (result > 0) result else 0
//
//                        if (System.currentTimeMillis() - lastUpdate > 100) {
//                            lastUpdate = System.currentTimeMillis()
//                            channel.send(DownloadStatus(
//                                    bytesRead.toFloat() / onlineFile.decompressedSize.toFloat() * 100f)
//                            )
//                        }
//                    }
//                    channel.send(DownloadStatus(isComplete = true))
//                }
//            }
//            Lg.i("time = $time ms")
//        } catch (e: IOException) {
//            Lg.i("IOException = $e")
//            channel.send(DownloadStatus(error = getError(e)))
//        } finally {
//            fileSink?.close()
//            channel.close()
//        }
//    }
//
//    private fun getError(e: IOException): Error {
//        val str = e.toString()
//        return when {
//            str.startsWith("java.net.UnknownHostException") -> UNKNOWN_HOST // Internet on but can't resolve hostname
//            str.startsWith("java.net.ConnectException") -> HOST_UNREACHABLE // Internet on but host ip unreachable
//            str.startsWith("java.net.SocketException") -> CONNECTION_LOST // Connection lost mid-way
//            str.startsWith("java.net.SocketTimeoutException") -> CONNECTION_TIMEOUT // Connection lost mid-way
//            else -> throw e
//        }
//    }
//
//
//    private fun getFileSink(onlineFile: OnlineFile): BufferedSink {
//        val dir = File(storageHelper.getLocalPath(onlineFile))
//        dir.mkdirs()
//        val file = File(dir.absolutePath, onlineFile.filename)
//        if (!file.exists())
//            file.createNewFile()
//        return file.sink().buffer()
//    }
//
//    private suspend fun getResponseBody(onlineFile: OnlineFile): ResponseBody {
//        val request: Request = Request.Builder()
//                .url(onlineFile.url)
//                .build()
//        val call: Call = okHttpClient.newCall(request)
//        val response = withContext(Dispatchers.IO) { call.execute() }
//        return response.body() ?: throw IllegalStateException()
//    }
//
//    data class DownloadStatus(
//            val progress: Float = 0f, // 0-100
//            val isComplete: Boolean = false,
//            val error: Error? = null
//    )
//
//    enum class Error {
//        NO_STORAGE, UNKNOWN_HOST, HOST_UNREACHABLE, FILE_SIZE_MISMATCH, CONNECTION_LOST, CONNECTION_TIMEOUT;
//
//        override fun toString(): String {
//            return when (this) {
//                NO_STORAGE -> "Storage unavailable"
//                UNKNOWN_HOST -> "Unknown host"
//                HOST_UNREACHABLE -> "Host unreachable"
//                FILE_SIZE_MISMATCH -> "File size mismatch"
//                CONNECTION_LOST -> "Connection lost"
//                CONNECTION_TIMEOUT -> "Connection timeout"
//            }
//        }
//    }