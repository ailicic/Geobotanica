package com.geobotanica.geobotanica.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.geobotanica.geobotanica.util.GbTime
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.threeten.bp.Instant


@Entity(tableName = "geolocations")
@JsonClass(generateAdapter = true)
data class Geolocation (
//        @Json(name = "geoplugin_request") val ipAddress: String,
        @Json(name = "geoplugin_city") val city: String = "",
        @Json(name = "geoplugin_region") val region: String = "", // Province / state
        @Json(name = "geoplugin_regionCode") val regionCode: String = "",
        @Json(name = "geoplugin_regionName") val regionName: String = "", // Province / state
        @Json(name = "geoplugin_countryCode") val countryCode: String = "",
        @Json(name = "geoplugin_countryName") val countryName: String = "",
        @Json(name = "geoplugin_continentCode") val continentCode: String = "",
        @Json(name = "geoplugin_continentName") val continentName: String = "",
        @Json(name = "geoplugin_latitude") val latitude: String = "",
        @Json(name = "geoplugin_longitude") val longitude: String = "",
        @Json(name = "geoplugin_locationAccuracyRadius") val locationAccuracyRadius: String = "",
        val timestamp: Instant = GbTime.now()
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
}

// Geolocation(city=Penticton, region=British Columbia, regionCode=BC,
// regionName=British Columbia, countryCode=CA, countryName=Canada,
// continentCode=NA, continentName=North America,
// latitude=49.4806, longitude=-119.5858, locationAccuracyRadius=100)
