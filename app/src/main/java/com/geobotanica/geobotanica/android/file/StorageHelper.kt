package com.geobotanica.geobotanica.android.file

import android.annotation.SuppressLint
import android.content.Context
import com.geobotanica.geobotanica.network.RemoteFile
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageHelper @Inject constructor(val appContext: Context) {

    fun isDownloaded(remoteFile: RemoteFile): Boolean {
        val file = File(getDownloadPath(), remoteFile.fileNameGzip)
        return file.exists() && file.isFile && file.length() == remoteFile.compressedSize
    }

    fun isDecompressed(remoteFile: RemoteFile): Boolean {
        val file = File(getLocalPath(remoteFile), remoteFile.fileName)
        return file.exists() && file.isFile && file.length() == remoteFile.decompressedSize
    }

    @SuppressLint("UsableSpace")
    fun isStorageAvailable(remoteFile: RemoteFile): Boolean {
        val dir = File(getRootPath(remoteFile))
        return dir.usableSpace > 2 * remoteFile.decompressedSize
    }

    fun mkdirs(remoteFile: RemoteFile) = File(getLocalPath(remoteFile)).mkdirs()

    fun getDownloadPath() = appContext.getExternalFilesDir(null)?.absolutePath

    fun getLocalPath(remoteFile: RemoteFile): String =
            getRootPath(remoteFile) + "/${remoteFile.relativePath}"

    private fun getRootPath(remoteFile: RemoteFile): String {
        return if (remoteFile.isInternalStorage)
            appContext.filesDir.absolutePath.removeSuffix("/files")
        else
            appContext.getExternalFilesDir(null)!!.absolutePath
    }
}
