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
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.di.PerActivity
import com.geobotanica.geobotanica.util.Lg
import javax.inject.Inject

typealias LocationCallback = (Location) -> Unit

@PerActivity
class LocationService @Inject constructor (private val locationManager: LocationManager) {
    private val observers = mutableSetOf<LocationCallback>()
    private val gpsLocationListener:GpsLocationListener = GpsLocationListener()
    private var gnssStatusCallback:GnssStatusCallback? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            gnssStatusCallback = GnssStatusCallback()
    }

    fun subscribe(callback: LocationCallback) {
        if(observers.isEmpty()) {
            registerGpsUpdates()
        }
        val isAdded = observers.add(callback)
        Lg.d("LocationService:subscribe(): isAdded=$isAdded, observers=${observers.count()}, callback=$callback")
    }

    fun unsubscribe(callback: LocationCallback) {
        val isRemoved = observers.remove(callback)
        if(observers.isEmpty())
            unregisterGpsUpdates()
        Lg.d("LocationService:unsubscribe(): isRemoved=$isRemoved, observers=${observers.count()}")
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
                        satellitesVisible = satellites,
                        precision = accuracy,
                        latitude = latitude,
                        longitude = longitude,
                        altitude = altitude
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
                        satellitesInUse = satellitesInUse,
                        satellitesVisible = satellitesVisible
                ))
            }
        }
    }
}
