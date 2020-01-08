@file:Suppress("DEPRECATION")

package com.geobotanica.geobotanica.android.location

import android.annotation.SuppressLint
import android.location.GnssStatus
import android.location.GpsStatus
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.isEmulator
import javax.inject.Inject
import javax.inject.Singleton

interface LocationSubscriber {
    fun onLocation(location: Location)
}

@Singleton
class LocationService @Inject constructor (private val locationManager: LocationManager) {
    private val subscribers = mutableListOf<LocationSubscriber>()
    private var currentInterval = 0L
    private var gnssStatusCallback: GnssStatusCallback? = null
    private val gpsLocationListener: GpsLocationListener = GpsLocationListener()
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
    fun isGpsSubscribed(observer: Any): Boolean = subscribers.contains(observer)

    fun subscribe(subscriber: LocationSubscriber, interval: Long = 0) {
        if (subscribers.isEmpty() || interval != currentInterval) {
            registerGpsUpdates(interval)
            currentInterval = interval
        }
        val isSubscribed = subscribers.add(subscriber)
        Lg.v("LocationService: subscribe(): isSubscribed=$isSubscribed, subscribers=${subscribers.count()}, interval=$interval, subscriber=$subscriber")
    }

    fun unsubscribe(observer: Any) {
        val isRemoved = subscribers.remove(observer)
        if(subscribers.isEmpty()) {
            unregisterGpsUpdates()
            hasFirstFix = false
        }
        Lg.v("LocationService: unsubscribe(): isRemoved=$isRemoved, subscribers=${subscribers.count()}, observer=$observer\"")
    }

    private fun publish(location: Location) {
        subscribers.forEach { it.onLocation(location) }
    }

    private fun onLocation(location: Location) {
        if (isEmulator()) {
            publish(location)
            return
        }
        location.latitude?.let { hasFirstFix = true }
        if (!hasFirstFix) {
            publish(location) // Send out satellite data
        } else { // Send out merged lat/long + sat data events only if received within 100 ms
            if (tempLocation == null) {
                tempLocation = location
                msSinceLastEvent = System.currentTimeMillis()
            } else {
                if (System.currentTimeMillis() - msSinceLastEvent  > maxMsToMerge) {
                    // Discard tempLocation, wait for next event
                    tempLocation = location
                    msSinceLastEvent = System.currentTimeMillis()
                } else { // Events arrived within 100 ms of each other. Merge them and publish()
                    tempLocation?.let { publish(location.mergeWith(it)) }
                    tempLocation = null
                }
            }

        }
    }

    @SuppressLint("MissingPermission")
    private fun registerGpsUpdates(interval: Long = 0) {
//        Lg.d("LocationService: registerGpsUpdates()")
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, 0f, gpsLocationListener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            gnssStatusCallback?.let { locationManager.registerGnssStatusCallback(it) }
//            val isAdded = locationManager.registerGnssStatusCallback(gnssStatusCallback)
//            Lg.v("Registering GPS status (API >= 24), isAdded=$isAdded, callback=$gnssStatusCallback")
        } else {
            locationManager.addGpsStatusListener(gpsStatusListener)
//            val isAdded = locationManager.addGpsStatusListener(gpsStatusListener)
//            Lg.v("Registering GPS status (API < 24), isAdded=$isAdded, callback=$gpsStatusListener")
        }
    }

    private fun unregisterGpsUpdates() {
//        Lg.d("LocationService: unregisterGpsUpdates()")
        locationManager.removeUpdates(gpsLocationListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            Lg.v("Unregistering GPS status (API >= 24), callback=$gnssStatusCallback")
            gnssStatusCallback?.let { locationManager.unregisterGnssStatusCallback(it) }
        } else {
//            Lg.v("Unregistering GPS status (API < 24), callback=$gpsStatusListener")
            locationManager.removeGpsStatusListener(gpsStatusListener)
        }
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
            Lg.v("GpsLocationListener(): OnStatusChanged(): status=$status")
        }

        override fun onProviderEnabled(provider: String) {
            Lg.i("GpsLocationListener(): OnProviderEnabled()")
        }

        override fun onProviderDisabled(provider: String) {
            Lg.i("GpsLocationListener(): OnProviderDisabled()")
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
            Lg.v("GnssStatus.Callback::onStarted()")
        }

        override fun onFirstFix(ttffMillis: Int) {
            super.onFirstFix(ttffMillis)
            Lg.v("GnssStatus.Callback::onFirstFix()")
        }

        override fun onStopped() {
            super.onStopped()
            Lg.v("GnssStatus.Callback::onStopped()")
        }
    }

    // For Android M (API 23) and prior
    @SuppressLint("MissingPermission")
    private inner class GpsStatusListener: GpsStatus.Listener {
        override fun onGpsStatusChanged(event: Int) {
            when (event) {
                GpsStatus.GPS_EVENT_STARTED-> Lg.v("GPS_EVENT_STARTED")
                GpsStatus.GPS_EVENT_STOPPED-> Lg.v("GPS_EVENT_STOPPED")
                GpsStatus.GPS_EVENT_FIRST_FIX->  Lg.v("GPS_EVENT_FIRST_FIX")
                GpsStatus.GPS_EVENT_SATELLITE_STATUS-> {
                    val status = locationManager.getGpsStatus(null)
                    val satellitesInUse = status?.satellites?.filter {it.usedInFix()}?.count() ?: 0
                    val satellitesVisible = status?.satellites?.count() ?: 0
//                    Lg.d("GPS_EVENT_SATELLITE_STATUS: $satellitesInUse/$satellitesVisible")
                    onLocation(Location(
                            satellitesInUse = satellitesInUse,
                            satellitesVisible = satellitesVisible
                    ))
                }
            }
        }
    }

//    @SuppressLint("MissingPermission")
//    fun getLastLocation(): Location? { // USELESS: Always returns null
//        val extractLocation: (android.location.Location) -> Location = {
//            Location(
//                    latitude = it.latitude,
//                    longitude = it.longitude,
//                    altitude = it.altitude,
//                    satellitesVisible = 0
//            )
//        }
//        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
//            Lg.d("getLastLocation(GPS_PROVIDER): $it")
//            return extractLocation(it)
//        }
//        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
//            Lg.d("getLastLocation(NETWORK_PROVIDER): $it")
//            return extractLocation(it)
//        }
//        Lg.d("getLastLocation(): None available")
//        return null
//    }
}
