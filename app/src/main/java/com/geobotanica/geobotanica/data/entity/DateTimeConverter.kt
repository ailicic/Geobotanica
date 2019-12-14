package com.geobotanica.geobotanica.data.entity

import androidx.room.TypeConverter
import org.threeten.bp.Instant


object DateTimeConverter {

    @TypeConverter
    @JvmStatic
    fun toInstant(string: String): Instant = Instant.parse(string)

    @TypeConverter
    @JvmStatic
    fun fromInstant(instant: Instant): String = instant.toString()
}