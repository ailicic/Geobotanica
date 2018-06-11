package com.geobotanica.geobotanica.android.location

import android.annotation.SuppressLint
import android.app.Application
import android.location.*
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat.requestPermissions
import com.geobotanica.geobotanica.util.Lg
import javax.inject.Inject

data class Location(
        val locationType: LocationService.LocationType,
        val lat: Double? = null,
        val long: Double? = null,
        val alt: Double? = null,
        val precision: Float? = null,
        val satellitesInUse: Int? = null,
        val satellitesVisible: Int? = null,
        val time: Long? = null
)

//class LocationService(val gpsService: GpsService, val networkLocationService: NetworkLocationService) {

typealias LocationCallback = (Location) -> Unit

class LocationService @Inject constructor (
        private val application: Application,
        private val locationManager: LocationManager)
{
    private val observers = mutableListOf<LocationCallback>()
    private val requestFineLocationPermission = 1

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

    init {
        if (isGpsPermitted()) {
            Lg.d("GPS already permitted")
            requestGpsUpdates()
        } else
            Lg.d("Requesting GPS permissions now...")
//        requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestFineLocationPermission)
    }

    fun subscribe(callback: LocationCallback) {
        Lg.d("LocationService: subscribe($callback)")
        observers.add(callback)
    }

    fun unsubscribe(callback: LocationCallback) = observers.remove(callback)

    private fun notify(location: Location) {
        observers.apply { (location) }
    }

    private fun isGpsPermitted(): Boolean {
//        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) ==
//                PackageManager.PERMISSION_GRANTED
        return true
    }

//    override fun onRequestPermissionsResult(requestCode: Int,
//                                            permissions: Array<String>, grantResults: IntArray) {
//        when (requestCode) {
//            requestFineLocationPermission -> {
//                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                    Lg.d("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_GRANTED")
//                    requestGpsUpdates()
//                } else {
//                    Lg.d("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_DENIED")
//                }
//            }
//            else -> { } // Ignore all other requests.
//        }
//    }

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
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {   Lg.d("GpsLocation: OnStatusChanged") }
        override fun onProviderEnabled(provider: String) {                              Lg.d("GpsLocation: OnProviderEnabled") }
        override fun onProviderDisabled(provider: String) {                             Lg.d("GpsLocation: OnProviderDisabled") }

    }

    // TODO: Check if all GPS related callbacks/listeners need to be unregistered in fragment lifecycle
    //        val locationManager: LocationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    //        locationManager.removeUpdates(GpsLocationListener)

    @RequiresApi(Build.VERSION_CODES.N)
    private inner class GnssStatusCallback : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus?) {
            super.onSatelliteStatusChanged(status)
            status?.let {
                val satellitesVisible = status.satelliteCount
                var satellitesInUse = 0
                for(i in 0 until satellitesVisible) {
                    satellitesInUse += if(status.usedInFix(i)) 1 else 0
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

    // TODO: Verify the implications of this request. It appears it also triggers the permission request.
    @SuppressLint("MissingPermission")
    private fun requestGpsUpdates() {
        Lg.d("Requesting GPS updates now...")
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, GpsLocationListener())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Lg.d("Registering GPS status (API >= 24)")
            locationManager.registerGnssStatusCallback(GnssStatusCallback())
        } else {
            Lg.d("Registering GPS status (API < 24)")
            locationManager.addGpsStatusListener(::onGpsStatusChanged)
        }


    }
}