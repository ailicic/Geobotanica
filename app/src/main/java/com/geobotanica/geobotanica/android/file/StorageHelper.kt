package com.geobotanica.geobotanica.android.file

import android.annotation.SuppressLint
import android.content.Context
import com.geobotanica.geobotanica.network.OnlineAsset
import com.geobotanica.geobotanica.network.OnlineAssetIndex
import com.geobotanica.geobotanica.network.OnlineAssetIndex.*
import com.geobotanica.geobotanica.network.onlineAssetList
import com.geobotanica.geobotanica.network.online_map.OnlineMapEntry
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageHelper @Inject constructor(val appContext: Context) {

    fun isAssetDownloaded(onlineAsset: OnlineAsset): Boolean {
        val file = File(getDownloadPath(), onlineAsset.fileNameGzip)
        return file.exists() && file.isFile && file.length() == onlineAsset.compressedSize
    }

    fun isAssetDecompressed(onlineAsset: OnlineAsset): Boolean {
        val file = File(getLocalPath(onlineAsset), onlineAsset.fileName)
        return file.exists() && file.isFile && file.length() == onlineAsset.decompressedSize
    }

    fun isMapDownloaded(onlineMapEntry: OnlineMapEntry): Boolean {
        val file = File(getLocalPath(onlineAssetList[WORLD_MAP.ordinal]), onlineMapEntry.filename)
        return file.exists() && file.isFile
    }

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
}
