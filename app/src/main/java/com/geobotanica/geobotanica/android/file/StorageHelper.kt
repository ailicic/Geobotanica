package com.geobotanica.geobotanica.android.file

import android.annotation.SuppressLint
import android.content.Context
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageHelper @Inject constructor(val appContext: Context) {

    @SuppressLint("UsableSpace")
    fun isStorageAvailable(onlineAsset: OnlineAsset): Boolean {
        val dir = File(getRootPath(onlineAsset))
        return dir.usableSpace > 2 * onlineAsset.decompressedSize
    }

    fun mkdirs(onlineAsset: OnlineAsset) = File(getLocalPath(onlineAsset)).mkdirs()

    fun getDownloadPath() = appContext.getExternalFilesDir(null)?.absolutePath

    fun getMapsPath() = "${getDownloadPath()}/maps"

    fun getLocalPath(onlineAsset: OnlineAsset): String =
            getRootPath(onlineAsset) + "/${onlineAsset.relativePath}"

    private fun getRootPath(onlineAsset: OnlineAsset): String {
        return if (onlineAsset.isInternalStorage)
            appContext.filesDir.absolutePath.removeSuffix("/files")
        else
            appContext.getExternalFilesDir(null)!!.absolutePath
    }

    fun isAssetAvailable(asset: OnlineAsset): Boolean {
        val file = File(getLocalPath(asset), asset.filename)
        return file.exists() && file.length() == asset.decompressedSize
    }
}
