package com.geobotanica.geobotanica.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.Plant.Type.*
import com.geobotanica.geobotanica.data.entity.PlantComposite
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.ui.map.MapViewModel.GpsFabState.*
import com.geobotanica.geobotanica.util.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Identify more concise way to expose immutable LiveData objects to view

@Singleton
class MapViewModel @Inject constructor(
    private val plantRepo: PlantRepo,
    private val locationService: LocationService
): ViewModel() {
    var userId = 0L    // Field injected dynamic parameter

    val plantMarkerData: LiveData< List<PlantMarkerData> > =
            map(plantRepo.getAllPlantComposites()) { extractPlantMarkerDataList(it) }

    val currentLocation: LiveData<Location>
        get() = _currentLocation
    private val _currentLocation = MutableLiveData<Location>()

    private val defaultMapZoomLevel = 16.0
    private val defaultMapLatitude = 49.477
    private val defaultMapLongitude = -119.59

    var isFirstRun = true
    var mapZoomLevel: Double = defaultMapZoomLevel
    var mapLatitude: Double = defaultMapLatitude
    var mapLongitude: Double = defaultMapLongitude
    var wasGpsSubscribed = true

    val gpsFabIcon = SingleLiveEvent<Int>() // TODO: SingleLiveEvent not required here-> User MutableLiveData
    val showGpsRequiredSnackbar = SingleLiveEvent<Unit>()
    val navigateToNewPlant = SingleLiveEvent<Unit>()

//    val gpsFabIcon: LiveData<FabGpsIcon>
//        get() = _gpsFabIcon
//    private val _gpsFabIcon = MutableLiveData<FabGpsIcon>()

    enum class GpsFabState { GPS_OFF, GPS_NO_FIX, GPS_FIX }
    private val gpsFabIcons: Map<GpsFabState, Int> = mapOf(
            GPS_OFF to R.drawable.gps_off,
            GPS_NO_FIX to R.drawable.gps_no_fix,
            GPS_FIX to R.drawable.gps_fix)


    fun onClickGpsFab() {
        if (!isGpsEnabled()) {
            gpsFabIcon.postValue(gpsFabIcons[GPS_OFF])
            showGpsRequiredSnackbar.call()
        }
        else if (isGpsSubscribed())
            unsubscribeGps()
        else
            subscribeGps()
    }

    fun onClickNewPlantFab() {
        navigateToNewPlant.call()
    }

    fun initGpsSubscribe() {
        val isGpsEnabled = isGpsEnabled()
        if (!isGpsEnabled)
            showGpsRequiredSnackbar.call()
        if (wasGpsSubscribed && isGpsEnabled )
            subscribeGps()
    }

    fun isGpsSubscribed(): Boolean = locationService.isGpsSubscribed(this)

    fun unsubscribeGps() {
        if (isGpsSubscribed()) {
            gpsFabIcon.postValue(gpsFabIcons[GPS_OFF])
            locationService.unsubscribe(this)
        }
    }

    private fun subscribeGps() {
        if (!isGpsSubscribed()) {
            gpsFabIcon.postValue(gpsFabIcons[GPS_NO_FIX])
            locationService.subscribe(this, ::onLocation, 5000)
        }
    }

    private fun isGpsEnabled(): Boolean = locationService.isGpsEnabled()

    private fun onLocation(location: Location) {
        _currentLocation.postValue(location)
    }

//    private fun getLastLocation(): Location? = locationService.getLastLocation()

    private fun extractPlantMarkerDataList(plantComposites: List<PlantComposite>): List<PlantMarkerData> =
            plantComposites.map { extractPlantMarkerData(it) }

    private fun extractPlantMarkerData(plantComposite: PlantComposite): PlantMarkerData {
        val plant = plantComposite.plant
        val photoPath = plantComposite.photos.first().fileName
        val location = plantComposite.plantLocations.first().location
        val plantIcon = getPlantIconFromType(plant.type)
        return PlantMarkerData(
            plant.id,
            plant.commonName,
            plant.latinName,
            location.latitude,
            location.longitude,
            photoPath,
            plant.timestamp.toString().substringBefore('T'),
            plantIcon
        )
    }

    private fun getPlantIconFromType(plantType: Plant.Type): Int {
        return when (plantType) {
            TREE -> R.drawable.marker_purple
            SHRUB -> R.drawable.marker_blue
            HERB -> R.drawable.marker_green
            GRASS -> R.drawable.marker_light_green
            VINE -> R.drawable.marker_yellow
        }
    }
}

data class PlantMarkerData(
    val plantId: Long,
    val commonName: String? = null,
    val latinName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoPath: String? = null,
    val dateCreated: String? = null,
    val icon: Int? = null
)