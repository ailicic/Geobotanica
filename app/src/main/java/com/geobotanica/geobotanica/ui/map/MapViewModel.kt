package com.geobotanica.geobotanica.ui.map

import androidx.lifecycle.*
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.util.Lg
import org.osmdroid.util.GeoPoint


class MapViewModel constructor(
        private var plantRepo: PlantRepo,
        private var locationService: LocationService
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

    var alreadyNotifiedGpsRequired = false
    var mapWasCenteredOnCurrentLocationOnce = false // TODO: Remove this after GPS button is added to map
    var mapZoomLevel: Double? = null
    var mapPosition: GeoPoint? = null


    private fun init() {
        Lg.d("MapViewModel: init()")
        allPlantComposites = plantRepo.getAllPlantComposites()
    }

    fun startGps() {
        locationService.subscribe(this, ::onLocation)
    }

    fun stopGps() {
        locationService.unsubscribe(this)
    }

    fun isGpsEnabled(): Boolean = locationService.isGpsEnabled()

    private fun onLocation(location: Location) {
//        Lg.v("MapViewModel:onLocation(): $location")
        _currentLocation.postValue(location)
    }
}