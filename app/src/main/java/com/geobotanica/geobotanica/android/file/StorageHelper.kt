package com.geobotanica.geobotanica.android.file

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.util.GbTime
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.asFilename
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageHelper @Inject constructor(val appContext: Context) {

    fun getAbsolutePath(file: File): String = file.absolutePath

    fun getLocalPath(onlineAsset: OnlineAsset): String =
            getRootPath(onlineAsset) + "/${onlineAsset.relativePath}"

    fun getDownloadPath() = getExtFilesDir()

    fun getExtStorageRootDir(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            appContext.getExternalFilesDirs(null)[0].absolutePath
        // TODO: Check if root external storage is accessible on Q+ in future
        else @Suppress("DEPRECATION")
            Environment.getExternalStorageDirectory().absolutePath // "/sdcard/"
    }

    @SuppressLint("UsableSpace")
    fun getFreeExternalStorageInMb() = File(getExtStorageRootDir()).usableSpace / 1024 / 1024

    fun deleteFile(uri: String): Boolean = File(uri).delete()

    @SuppressLint("UsableSpace")
    fun isStorageAvailable(onlineAsset: OnlineAsset): Boolean {
        val dir = File(getRootPath(onlineAsset))
        return dir.usableSpace > 2 * onlineAsset.decompressedSize
    }

    fun mkdirs(onlineAsset: OnlineAsset) = File(getLocalPath(onlineAsset)).mkdirs()

    fun isAssetAvailable(asset: OnlineAsset): Boolean {
        val file = File(getLocalPath(asset), asset.filename)
        return file.exists() && file.length() == asset.decompressedSize
    }

    fun isGzipAssetInExtStorageRootDir(asset: OnlineAsset): Boolean {
        return File(getExtStorageRootDir(), asset.filenameGzip).run {
            var result = true
            if (exists() && length() == asset.compressedSize)
                Lg.d("Found asset on external storage: $path")
            else if (! exists()) {
                Lg.d("Failed to find asset on external storage: $path")
                result = false
            } else {
                Lg.d("Wrong file size of asset on external storage: $path (expected ${asset.compressedSize} b, found ${length()} b)")
                result = false
            }
            result
        }
    }

    fun getMapsPath() = "${getDownloadPath()}/maps"

    fun createPhotoFile(): File {
        val filename: String = GbTime.now().asFilename()
        val photosDir = File(getUserPhotosDir())
        photosDir.mkdirs()
        Lg.d("StorageHelper.createPhotoFile(): $photosDir/$filename.jpg")
        return File.createTempFile(filename, ".jpg", photosDir)
    }

    fun photoUriFrom(filename: String): String = "${getUserPhotosDir()}/$filename"

    private fun getRootPath(onlineAsset: OnlineAsset): String {
        return if (onlineAsset.isInternalStorage)
            appContext.filesDir.absolutePath.removeSuffix("/files")
        else
            appContext.getExternalFilesDir(null)?.absolutePath  ?: throw IllegalStateException()
    }

    private fun getExtFilesDir() = appContext.getExternalFilesDir(null)?.absolutePath
    // /storage/emulated/0/Android/data/com.geobotanica/files/

    private fun getUserPhotosDir() = "${getPicturesDir()}"
    //  /storage/emulated/0/Android/data/com.geobotanica/files/Pictures/

    private fun getPicturesDir() = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath
    //  /storage/emulated/0/Android/data/com.geobotanica/files/Pictures/
}
