package com.geobotanica.geobotanica.data.entity

import androidx.room.TypeConverter
import com.geobotanica.geobotanica.network.DownloadStatus


object DownloadStatusConverter {

    @TypeConverter
    @JvmStatic
    fun toDownloadStatus(ordinal: Int): DownloadStatus = DownloadStatus.values()[ordinal]

    @TypeConverter
    @JvmStatic
    fun fromDownloadStatus(downloadStatus: DownloadStatus): Int = downloadStatus.ordinal
}