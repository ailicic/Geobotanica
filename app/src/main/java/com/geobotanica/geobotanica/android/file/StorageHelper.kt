package com.geobotanica.geobotanica.android.file

import android.annotation.SuppressLint
import android.content.Context
import com.geobotanica.geobotanica.network.OnlineFile
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageHelper @Inject constructor(val appContext: Context) {

    fun isDownloaded(onlineFile: OnlineFile): Boolean {
        val file = File(getDownloadPath(), onlineFile.fileNameGzip)
        return file.exists() && file.isFile && file.length() == onlineFile.compressedSize
    }

    fun isDecompressed(onlineFile: OnlineFile): Boolean {
        val file = File(getLocalPath(onlineFile), onlineFile.fileName)
        return file.exists() && file.isFile && file.length() == onlineFile.decompressedSize
    }

    @SuppressLint("UsableSpace")
    fun isStorageAvailable(onlineFile: OnlineFile): Boolean {
        val dir = File(getRootPath(onlineFile))
        return dir.usableSpace > 2 * onlineFile.decompressedSize
    }

    fun mkdirs(onlineFile: OnlineFile) = File(getLocalPath(onlineFile)).mkdirs()

    fun getDownloadPath() = appContext.getExternalFilesDir(null)?.absolutePath

    fun getLocalPath(onlineFile: OnlineFile): String =
            getRootPath(onlineFile) + "/${onlineFile.relativePath}"

    private fun getRootPath(onlineFile: OnlineFile): String {
        return if (onlineFile.isInternalStorage)
            appContext.filesDir.absolutePath.removeSuffix("/files")
        else
            appContext.getExternalFilesDir(null)!!.absolutePath
    }
}
