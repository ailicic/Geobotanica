package com.geobotanica.geobotanica.android.file

import android.annotation.SuppressLint
import android.content.Context
import com.geobotanica.geobotanica.network.RemoteFile
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageHelper @Inject constructor(val appContext: Context) {

    @SuppressLint("UsableSpace")
    fun isStorageAvailable(remoteFile: RemoteFile): Boolean {
        val dir = File(getLocalPath(remoteFile))
        return dir.usableSpace > remoteFile.decompressedSize * 1.5f
    }

    fun getLocalPath(remoteFile: RemoteFile): String {
        return if (remoteFile.isExternalStorage)
            appContext.getExternalFilesDir(null)!!.absolutePath + "/${remoteFile.relativePath}"
        else
            appContext.filesDir.absolutePath.removeSuffix("/files") + "/${remoteFile.relativePath}"
    }
}