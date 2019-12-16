package com.geobotanica.geobotanica.network

import com.geobotanica.geobotanica.data.entity.Geolocation
import com.geobotanica.geobotanica.util.adapter
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

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun get(): Geolocation {
        val adapter = moshi.adapter<Geolocation>()
        return adapter.fromJson(okHttpFileDownloader.getJson(geolocatorUrl)) ?: throw IllegalStateException()
    }
}
