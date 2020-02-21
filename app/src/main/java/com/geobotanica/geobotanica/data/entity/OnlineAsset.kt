package com.geobotanica.geobotanica.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.geobotanica.geobotanica.network.DownloadStatus
import com.geobotanica.geobotanica.network.DownloadStatus.*
import kotlin.math.roundToLong


@Entity(tableName = "assets")
//@JsonClass(generateAdapter = true)
data class  OnlineAsset(
        val description: String,
        val url: String, // Remote
        val relativePath: String, // Local
        val isInternalStorage: Boolean,
        val fileSize: Long,
        val decompressedSize: Long,
        val itemCount: Int,
        val status: DownloadStatus = NOT_DOWNLOADED,

        @PrimaryKey(autoGenerate = true)
        val id: Long = 0L
) {
    val filename: String get() = url.substringAfterLast('/')

    val filenameUngzip: String get() = filename.removeSuffix(".gz")

    val printName: String
        get() = "$description (${(fileSize.toFloat() / 1024 / 1024).roundToLong()} MB)"

    val isNotDownloaded: Boolean get() = status == NOT_DOWNLOADED
    val isDownloading: Boolean get() = status == DOWNLOADING
    val isDownloaded: Boolean get() = status == DOWNLOADED

    val shouldDecompress: Boolean get() = url.endsWith(".gz")
    val shouldDeserialize: Boolean get() = filenameUngzip.endsWith(".json")
}