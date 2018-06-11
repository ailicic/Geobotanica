@file:Suppress("DEPRECATION")

package com.geobotanica.geobotanica.android.location

import android.annotation.SuppressLint
import android.location.GnssStatus
import android.location.GpsStatus
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import com.geobotanica.geobotanica.util.Lg
import javax.inject.Inject

typealias LocationCallback = (Location) -> Unit

class LocationService @Inject constructor (private val locationManager: LocationManager) {
    private val observers = mutableListOf<LocationCallback>()
    private val gpsLocationListener:GpsLocationListener = GpsLocationListener()
    private var gnssStatusCallback:GnssStatusCallback? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            gnssStatusCallback = GnssStatusCallback()
    }

    fun subscribe(callback: LocationCallback) {
        Lg.d("LocationService: subscribe()")
        if(observers.isEmpty()) {
            registerGpsUpdates()
        }
        observers.add(callback)
    }

    fun unsubscribe(callback: LocationCallback) {
        observers.remove(callback)
        if(observers.isEmpty())
            unregisterGpsUpdates()
    }

    private fun notify(location: Location) {
        observers.forEach { it(location) }
    }

    @SuppressLint("MissingPermission")
    private fun registerGpsUpdates() {
        Lg.d("LocationService: registerGpsUpdates()")
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, gpsLocationListener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Lg.d("Registering GPS status (API >= 24)")
            locationManager.registerGnssStatusCallback(gnssStatusCallback)
        } else {
            Lg.d("Registering GPS status (API < 24)")
            locationManager.addGpsStatusListener(::onGpsStatusChanged)
        }
    }

    private fun unregisterGpsUpdates() {
        Lg.d("LocationService: unregisterGpsUpdates()")
        locationManager.removeUpdates(gpsLocationListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        } else {
            locationManager.removeGpsStatusListener(::onGpsStatusChanged)
        }
    }

    private inner class GpsLocationListener : LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            with(location) {
                val satellites = extras.getInt("satellitesInUse")  // Not used here. See GPS status listeners
                Lg.d("GpsLocationListener(): Accuracy = $accuracy, Satellites = $satellites, " +
                        "Lat = $latitude, Long = $longitude, Alt = $altitude")
                notify(Location(
                        LocationType.CURRENT_GPS_LOCATION,
                        satellitesVisible = satellites,
                        precision = accuracy,
                        lat = latitude,
                        long = longitude,
                        alt = altitude,
                        time = time
                ))
            }
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Lg.d("GpsLocationListener(): OnStatusChanged()")
        }

        override fun onProviderEnabled(provider: String) {
            Lg.d("GpsLocationListener(): OnProviderEnabled()")
        }

        override fun onProviderDisabled(provider: String) {
            Lg.d("GpsLocationListener(): OnProviderDisabled()")
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private inner class GnssStatusCallback : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus?) {
            super.onSatelliteStatusChanged(status)
            status?.let {
                val satellitesVisible = status.satelliteCount
                var satellitesInUse = 0
                for(i in 0 until satellitesVisible) {
                    if(status.usedInFix(i))
                        ++satellitesInUse
                }
                Lg.d("GnssStatus.Callback::onSatelliteStatusChanged(): $satellitesInUse/$satellitesVisible")
                notify(Location(
                        LocationType.CURRENT_GPS_LOCATION,
                        satellitesInUse = satellitesInUse,
                        satellitesVisible = satellitesVisible
                ))
            }
        }

        override fun onStarted() {
            super.onStarted()
            Lg.d("GnssStatus.Callback::onStarted()")
        }

        override fun onFirstFix(ttffMillis: Int) {
            super.onFirstFix(ttffMillis)
            Lg.d("GnssStatus.Callback::onFirstFix()")
        }

        override fun onStopped() {
            super.onStopped()
            Lg.d("GnssStatus.Callback::onStopped()")
        }
    }


    @SuppressLint("MissingPermission")
    private fun onGpsStatusChanged(event: Int) {
        when (event) {
            GpsStatus.GPS_EVENT_STARTED-> Lg.d("GPS_EVENT_STARTED")
            GpsStatus.GPS_EVENT_STOPPED-> Lg.d("GPS_EVENT_STOPPED")
            GpsStatus.GPS_EVENT_FIRST_FIX-> Lg.d("GPS_EVENT_FIRST_FIX")
            GpsStatus.GPS_EVENT_SATELLITE_STATUS-> {
                val status = locationManager.getGpsStatus(null)
                val satellitesInUse = status.satellites.filter({it.usedInFix()}).count()
                val satellitesVisible = status.satellites.count()
                Lg.d("GPS_EVENT_SATELLITE_STATUS: $satellitesInUse/$satellitesVisible")
                notify(Location(
                        LocationType.CURRENT_GPS_LOCATION,
                        satellitesInUse = satellitesInUse,
                        satellitesVisible = satellitesVisible
                ))
            }
        }
    }
}

data class Location(
        val locationType: LocationType,
        val lat: Double? = null,
        val long: Double? = null,
        val alt: Double? = null,
        val precision: Float? = null,
        val satellitesInUse: Int? = null,
        val satellitesVisible: Int? = null,
        val time: Long? = null
)

enum class LocationType {
    CURRENT_GPS_LOCATION,
    BEST_GPS_LOCATION,
    NETWORK_LOCATION,
    CACHED_LOCATION;

    override fun toString() =  when(this) {
        CURRENT_GPS_LOCATION -> "Current GPS"
        BEST_GPS_LOCATION-> "Best GPS"
        NETWORK_LOCATION -> "Network"
        CACHED_LOCATION -> "Cached"
    }
}