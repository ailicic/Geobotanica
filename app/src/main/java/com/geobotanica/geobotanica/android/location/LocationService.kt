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
import kotlin.math.max

typealias LocationCallback = (Location) -> Unit

@PerActivity
class LocationService @Inject constructor (private val locationManager: LocationManager) {
    private val observers = mutableSetOf<LocationCallback>()
    private var gnssStatusCallback:GnssStatusCallback? = null
    private val gpsLocationListener:GpsLocationListener = GpsLocationListener()
    private val gpsStatusListener = GpsStatusListener()

    private var hasFirstFix = false
    private var tempLocation: Location? = null
    private var msSinceLastEvent = Long.MAX_VALUE
    private val maxMsToMerge = 100L

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            gnssStatusCallback = GnssStatusCallback()
    }

    fun isGpsEnabled(): Boolean = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

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
            hasFirstFix = false
        Lg.d("LocationService: unsubscribe(): isRemoved=$isRemoved, observers=${observers.count()}")
    }

    private fun onLocation(location: Location) {
        location.latitude?.let { hasFirstFix = true }
        if (!hasFirstFix) {
            notify(location)
        } else {
            if (tempLocation == null) {
                tempLocation = location
                msSinceLastEvent = System.currentTimeMillis()
            } else {
                if (System.currentTimeMillis() - msSinceLastEvent  > maxMsToMerge) {
                    // Discard tempLocation, wait for next event
                    tempLocation = location
                    msSinceLastEvent = System.currentTimeMillis()
                } else {
                    // Events arrived within 100 ms of eachother. Merge them and notify()
                    notify( Location(
                            null,
                            location.latitude ?: tempLocation?.latitude,
                            location.longitude ?: tempLocation?.longitude,
                            location.altitude ?: tempLocation?.altitude,
                            location.precision ?: tempLocation?.precision,
                            location.satellitesInUse ?: tempLocation?.satellitesInUse,
                            satellitesVisible = max(location.satellitesVisible, tempLocation?.satellitesVisible ?: 0)
                    ))
                    tempLocation = null
                }
            }

        }
    }

    @SuppressLint("MissingPermission")
    private fun registerGpsUpdates() {
        Lg.d("LocationService: registerGpsUpdates()")
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, gpsLocationListener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val isAdded = locationManager.registerGnssStatusCallback(gnssStatusCallback)
            Lg.d("Registering GPS status (API >= 24), isAdded=$isAdded, callback=$gnssStatusCallback")
        } else {
            val isAdded = locationManager.addGpsStatusListener(gpsStatusListener)
            Lg.d("Registering GPS status (API < 24), isAdded=$isAdded, callback=$gpsStatusListener")
        }
    }

    private fun unregisterGpsUpdates() {
        Lg.d("LocationService: unregisterGpsUpdates()")
        locationManager.removeUpdates(gpsLocationListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Lg.d("Unregistering GPS status (API >= 24), callback=$gnssStatusCallback")
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        } else {
            Lg.d("Unregistering GPS status (API < 24), callback=$gpsStatusListener")
            locationManager.removeGpsStatusListener(gpsStatusListener)
        }
    }

    // TODO: Push merge code into Location repo: location.mergeWith(location)
    private fun notify(location: Location) {
        observers.forEach { it(location) }
    }

    private inner class GpsLocationListener : LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            with(location) {
                val satellites = extras.getInt("satellitesInUse")
//                Lg.v("GpsLocationListener(): Accuracy = $accuracy, Satellites = $satellites, " +
//                        "Lat = $latitude, Long = $longitude, Alt = $altitude")
                onLocation(Location(
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

    @RequiresApi(Build.VERSION_CODES.N) // API 24
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
//                Lg.v("GnssStatus.Callback::onSatelliteStatusChanged(): $satellitesInUse/$satellitesVisible")
                onLocation(Location(
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

    // For Android M (API 23) and prior
    @SuppressLint("MissingPermission")
    private inner class GpsStatusListener: GpsStatus.Listener {
        override fun onGpsStatusChanged(event: Int) {
            when (event) {
                GpsStatus.GPS_EVENT_STARTED-> Lg.d("GPS_EVENT_STARTED")
                GpsStatus.GPS_EVENT_STOPPED-> Lg.d("GPS_EVENT_STOPPED")
                GpsStatus.GPS_EVENT_FIRST_FIX->  Lg.d("GPS_EVENT_FIRST_FIX")
                GpsStatus.GPS_EVENT_SATELLITE_STATUS-> {
                    val status = locationManager.getGpsStatus(null)
                    val satellitesInUse = status.satellites.filter {it.usedInFix()}.count()
                    val satellitesVisible = status.satellites.count()
//                    Lg.d("GPS_EVENT_SATELLITE_STATUS: $satellitesInUse/$satellitesVisible")
                    onLocation(Location(
                            satellitesInUse = satellitesInUse,
                            satellitesVisible = satellitesVisible
                    ))
                }
            }
        }
    }
}
