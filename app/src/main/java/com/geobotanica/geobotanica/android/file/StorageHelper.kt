package com.geobotanica.geobotanica.android.file

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.util.Lg
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
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

    fun getExtStorageRootDir() = "/sdcard/"

    fun getExtFilesDir() = appContext.getExternalFilesDir(null)?.absolutePath
    // /storage/emulated/0/Android/data/com.geobotanica/files/

    fun getDownloadPath() = getExtFilesDir()

    fun getPicturesDir() = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath
    //  /storage/emulated/0/Android/data/com.geobotanica/files/Pictures/

    fun getUserPhotosDir() = "${getPicturesDir()}/"
    //  /storage/emulated/0/Android/data/com.geobotanica/files/Pictures/

    fun createPhotoFile(): File {
        val filename: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val photosDir = File(getUserPhotosDir())
        photosDir.mkdirs()
        Lg.d("StorageHelper.createPhotoFile(): $photosDir/$filename.jpg")
        return File.createTempFile(filename, ".jpg", photosDir)
    }

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

    fun photoUriFrom(filename: String): String = "${getUserPhotosDir()}/$filename"
}
