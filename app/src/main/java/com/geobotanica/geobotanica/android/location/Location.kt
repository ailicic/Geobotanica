package com.geobotanica.geobotanica.android.location

import com.geobotanica.geobotanica.util.GbTime
import org.mapsforge.core.model.LatLong
import org.threeten.bp.Instant
import kotlin.math.max


data class Location @JvmOverloads constructor(
        val latitude: Double? = null,
        val longitude: Double? = null,
        val altitude: Double? = null,
        val precision: Float? = null,
        val satellitesInUse: Int? = null,
        val satellitesVisible: Int, // Before GPS fix, visible satellites are always available
        val timestamp: Instant = GbTime.now()
) {
    fun isRecent(): Boolean = GbTime.now().minusSeconds(1) < timestamp

    fun mergeWith(location: Location): Location {
        return Location(
                latitude ?: location.latitude,
                longitude ?: location.longitude,
                altitude ?: location.altitude,
                precision ?: location.precision,
                satellitesInUse ?: location.satellitesInUse,
                satellitesVisible = max(satellitesVisible, location.satellitesVisible)
        )
    }

    fun toLatLong() = LatLong(latitude!!, longitude!!)
}