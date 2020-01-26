package com.geobotanica.geobotanica.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DOWNLOADED
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.NOT_DOWNLOADED
import kotlin.math.roundToLong


@Entity(tableName = "assets")
//@JsonClass(generateAdapter = true)
data class  OnlineAsset(
        val description: String,
        val url: String, // Remote
        val relativePath: String, // Local
        val isInternalStorage: Boolean,
        val compressedSize: Long,
        val decompressedSize: Long,
        var status: Long = NOT_DOWNLOADED
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L

    val filenameGzip: String
        get() = url.substringAfterLast('/')

    val filename: String
        get() = filenameGzip.removeSuffix(".gz")

    val printName: String
        get() = "$description (${(compressedSize.toFloat() / 1024 / 1024).roundToLong()} MB)"

    val isDownloading: Boolean
        get() = status > 0L

    val isDownloaded: Boolean
        get() = status == DOWNLOADED

    val downloadId: Long
        get() = status
}

// TODO: This is fragile. If asset list changes in updated version it will break.
enum class OnlineAssetId(val id: Long) {
    MAP_FOLDER_LIST(1L), MAP_LIST(2L), WORLD_MAP(3L), PLANT_NAMES(4L)
}