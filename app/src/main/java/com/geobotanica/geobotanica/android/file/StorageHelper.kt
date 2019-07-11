package com.geobotanica.geobotanica.android.file

import android.annotation.SuppressLint
import android.content.Context
import com.geobotanica.geobotanica.network.OnlineFile
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageHelper @Inject constructor(val appContext: Context) {

    fun isDownloaded(remoteFile: OnlineFile): Boolean {
        val file = File(getDownloadPath(), remoteFile.fileNameGzip)
        return file.exists() && file.isFile && file.length() == remoteFile.compressedSize
    }

    fun isDecompressed(remoteFile: OnlineFile): Boolean {
        val file = File(getLocalPath(remoteFile), remoteFile.fileName)
        return file.exists() && file.isFile && file.length() == remoteFile.decompressedSize
    }

    @SuppressLint("UsableSpace")
    fun isStorageAvailable(remoteFile: OnlineFile): Boolean {
        val dir = File(getRootPath(remoteFile))
        return dir.usableSpace > 2 * remoteFile.decompressedSize
    }

    fun mkdirs(remoteFile: OnlineFile) = File(getLocalPath(remoteFile)).mkdirs()

    fun getDownloadPath() = appContext.getExternalFilesDir(null)?.absolutePath

    fun getLocalPath(remoteFile: OnlineFile): String =
            getRootPath(remoteFile) + "/${remoteFile.relativePath}"

    private fun getRootPath(remoteFile: OnlineFile): String {
        return if (remoteFile.isInternalStorage)
            appContext.filesDir.absolutePath.removeSuffix("/files")
        else
            appContext.getExternalFilesDir(null)!!.absolutePath
    }
}
