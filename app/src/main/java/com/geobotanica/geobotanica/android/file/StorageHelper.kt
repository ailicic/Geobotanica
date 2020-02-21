package com.geobotanica.geobotanica.android.file

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.util.GbTime
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.asFilename
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageHelper @Inject constructor(val appContext: Context) {

    fun getAbsolutePath(file: File): String = file.absolutePath

    fun getDownloadPath() = getExtFilesPath()

    fun getExtStorageRootPath(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            appContext.getExternalFilesDirs(null)[0].absolutePath // TODO: Check if root external storage is accessible on Q+ in future
        else @Suppress("DEPRECATION")
            Environment.getExternalStorageDirectory().absolutePath // "/sdcard/"
    }

    // WARNING: This does not include space required by active downloads (i.e. DownloadManager does not pre-allocate space)
    @SuppressLint("UsableSpace")
    fun getFreeExternalStorageInMb() = File(getExtStorageRootPath()).usableSpace / 1024 / 1024

    fun deleteFile(uri: String): Boolean = File(uri).delete()

    fun getLocalPath(onlineAsset: OnlineAsset): String =
            getRootPath(onlineAsset) + "/${onlineAsset.relativePath}"

    @SuppressLint("UsableSpace")
    fun isStorageAvailable(onlineAsset: OnlineAsset): Boolean {
        val dir = File(getRootPath(onlineAsset))
        return dir.usableSpace > 3 * onlineAsset.decompressedSize
    }

    fun isGzipAssetInExtStorageRootDir(asset: OnlineAsset): Boolean {
        return File(getExtStorageRootPath(), asset.filename).run {
            var result = true
            if (exists() && length() == asset.fileSize)
                Lg.d("Found asset on external storage: $path")
            else if (! exists()) {
                Lg.d("Failed to find asset on external storage: $path")
                result = false
            } else {
                Lg.d("Wrong file size of asset on external storage: $path (expected ${asset.fileSize} b, found ${length()} b)")
                result = false
            }
            result
        }
    }

    @SuppressLint("UsableSpace")
    fun isStorageAvailable(onlineMap: OnlineMap): Boolean {
        val requiredStorageMb = (onlineMap.sizeMb * 2L)
        val freeStorageMb = getFreeExternalStorageInMb()
        return if (requiredStorageMb > freeStorageMb) {
            Lg.e("Insufficient storage ($freeStorageMb MB) for map: ${onlineMap.printName})")
            false
        } else
            true
    }

    fun getMapsPath() = "${getDownloadPath()}/maps"

    fun isMapOnExtStorage(map: OnlineMap): Boolean {
        val result = File(getExtStorageRootPath()).listFiles()?.any { it.name == map.filenameGzip } ?: false
        Lg.v("isMapOnExtStorage(): ${map.filename} (result=$result)")
        return result
    }

    fun createPhotoFile(): File {
        val filename: String = GbTime.now().asFilename()
        val photosDir = File(getPicturesPath())
        photosDir.mkdirs()
        Lg.d("StorageHelper.createPhotoFile(): $photosDir/$filename.jpg")
        return File.createTempFile(filename, ".jpg", photosDir)
    }

    fun photoUriFrom(filename: String): String = "${getPicturesPath()}/$filename"

    private fun getRootPath(onlineAsset: OnlineAsset): String {
        return if (onlineAsset.isInternalStorage)
            getPrivateStorageRootPath()
        else
            getExtFilesPath() ?: throw IllegalStateException()
    }

    private fun getPrivateStorageRootPath() = appContext.filesDir.absolutePath.removeSuffix("/files")

    private fun getExtFilesPath() = appContext.getExternalFilesDir(null)?.absolutePath
    // /storage/emulated/0/Android/data/com.geobotanica/files/

    private fun getPicturesPath() = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath
            ?: throw java.lang.IllegalStateException("External files dir not available")
    // /storage/emulated/0/Android/data/com.geobotanica/files/Pictures/
}
