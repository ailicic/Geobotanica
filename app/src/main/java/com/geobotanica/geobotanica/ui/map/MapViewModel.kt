package com.geobotanica.geobotanica.ui.map

import androidx.lifecycle.*
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.util.Lg
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapViewModel @Inject constructor(
        private val plantRepo: PlantRepo,
        private val locationService: LocationService
): ViewModel() {
    var userId = 0L    // Field injection of dynamic parameter.
        set(value) {
            field = value
            init()
        }

    lateinit var allPlantComposites: LiveData<List<PlantComposite>>

    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location>
        get() = _currentLocation

    // Map Defaults
    private val defaultMapZoomLevel = 16.0
    private val defaultMapLatitude = 49.477
    private val defaultMapLongitude = -119.59

    var isFirstRun = true
    var wasNotifiedGpsRequired = false
    var mapZoomLevel: Double = defaultMapZoomLevel
    var mapLatitude: Double = defaultMapLatitude
    var mapLongitude: Double = defaultMapLongitude
    var wasGpsSubscribed = true

    private fun init() {
        Lg.d("MapViewModel: init(userId=$userId)")
        allPlantComposites = plantRepo.getAllPlantComposites()
    }

    fun subscribeGps() {
        if (!isGpsSubscribed())
            locationService.subscribe(this, ::onLocation, 5000)
    }

    fun unsubscribeGps() {
        if (isGpsSubscribed())
            locationService.unsubscribe(this)
    }

    fun isGpsEnabled(): Boolean = locationService.isGpsEnabled()

    fun isGpsSubscribed(): Boolean = locationService.isGpsSubscribed(this)

    fun getLastLocation(): Location? = locationService.getLastLocation()

    private fun onLocation(location: Location) {
//        Lg.v("MapViewModel:onLocation(): $location")
        _currentLocation.postValue(location)
    }
}