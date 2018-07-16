package com.geobotanica.geobotanica.data.entity

import androidx.room.TypeConverter
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter

// https://medium.com/@chrisbanes/room-time-2b4cf9672b98
// http://www.threeten.org/threetenbp/apidocs/org/threeten/bp/OffsetDateTime.html

object OffsetDateTimeConverter {
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    @TypeConverter
    @JvmStatic
    fun toOffsetDateTime(value: String): OffsetDateTime = formatter.parse(value, OffsetDateTime::from)

    @TypeConverter
    @JvmStatic
    fun fromOffsetDateTime(date: OffsetDateTime): String = date.format(formatter)
}

/**
 * Converts OffsetDateTime to a String formatted as YYYY-MM-DD
 */
fun OffsetDateTime.toSimpleDate(): String = this.format(DateTimeFormatter.ISO_LOCAL_DATE)
