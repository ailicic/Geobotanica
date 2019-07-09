package com.geobotanica.geobotanica.network

import com.geobotanica.geobotanica.util.adapter
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Consider defining a generic Geolocation data class that adapts to different geolocation services
// This would allow failover across multiple services

@Singleton
class Geolocator @Inject constructor(
        private val okHttpFileDownloader: OkHttpFileDownloader,
        private val moshi: Moshi
) {
    private val geolocatorUrl = "http://www.geoplugin.net/json.gp"

    suspend fun get(): Geolocation {
        val adapter = moshi.adapter<Geolocation>()
        return adapter.fromJson(okHttpFileDownloader.getJson(geolocatorUrl))!!
    }
}

@JsonClass(generateAdapter = true)
data class Geolocation (
//        @Json(name = "geoplugin_request") val ipAddress: String,
        @Json(name = "geoplugin_city") val city: String,
        @Json(name = "geoplugin_region") val region: String, // Province / state
        @Json(name = "geoplugin_regionCode") val regionCode: String,
        @Json(name = "geoplugin_regionName") val regionName: String, // Province / state
        @Json(name = "geoplugin_countryCode") val countryCode: String,
        @Json(name = "geoplugin_countryName") val countryName: String,
        @Json(name = "geoplugin_continentCode") val continentCode: String,
        @Json(name = "geoplugin_continentName") val continentName: String,
        @Json(name = "geoplugin_latitude") val latitude: String,
        @Json(name = "geoplugin_longitude") val longitude: String,
        @Json(name = "geoplugin_locationAccuracyRadius") val locationAccuracyRadius: String
)
// Geolocation(city=Penticton, region=British Columbia, regionCode=BC,
// regionName=British Columbia, countryCode=CA, countryName=Canada,
// continentCode=NA, continentName=North America,
// latitude=49.4806, longitude=-119.5858, locationAccuracyRadius=100)
