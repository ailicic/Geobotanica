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
import com.geobotanica.geobotanica.ui.map.MapViewModel.GpsFabDrawable.GPS_NO_FIX
import com.geobotanica.geobotanica.ui.map.MapViewModel.GpsFabDrawable.GPS_OFF
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SingleLiveEvent
import com.geobotanica.geobotanica.util.schedule
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class PlantMarkerData(
        val plantId: Long,
        val commonName: String? = null,
        val scientificName: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val photoPath: String? = null,
        val dateCreated: String? = null,
        val icon: Int? = null
)

// TODO: Identify more concise way to expose immutable LiveData objects to view
@Singleton
class MapViewModel @Inject constructor(
    private val plantRepo: PlantRepo,
    private val locationService: LocationService
): ViewModel() {
    var userId = 0L    // Field injected dynamic parameter

    val plantMarkerData: LiveData< List<PlantMarkerData> > by lazy {
        map(plantRepo.getAllPlantComposites()) { extractPlantMarkerDataList(it) }
    }

    val currentLocation: LiveData<Location>
        get() = _currentLocation
    private val _currentLocation = MutableLiveData<Location>()

    val gpsFabIcon: LiveData<Int>
        get() = _gpsFabIcon
    private val _gpsFabIcon = MutableLiveData<Int>()

    val showGpsRequiredSnackbar = SingleLiveEvent<Unit>()
    val navigateToNewPlant = SingleLiveEvent<Unit>()

    private val defaultMapZoomLevel = 16.0
    private val defaultMapLatitude = 49.477
    private val defaultMapLongitude = -119.59

    var isFirstRun = true
    var mapZoomLevel: Double = defaultMapZoomLevel
    var mapLatitude: Double = defaultMapLatitude
    var mapLongitude: Double = defaultMapLongitude
    var wasGpsSubscribed = true

    private var staleGpsTimer: Timer? = null
    private val staleGpsTimeout = 120_000L // ms

    enum class GpsFabDrawable(val drawable: Int) {
        GPS_OFF(R.drawable.gps_off),
        GPS_NO_FIX(R.drawable.gps_no_fix),
        GPS_FIX(R.drawable.gps_fix)
    }

    fun initGpsSubscribe() {
        val isGpsEnabled = isGpsEnabled()
        if (!isGpsEnabled)
            showGpsRequiredSnackbar.call()
        else if (wasGpsSubscribed)
            subscribeGps()
    }

    fun onClickGpsFab() {
        if (!isGpsEnabled()) {
            _gpsFabIcon.value = GPS_OFF.drawable
            showGpsRequiredSnackbar.call()
        }
        else if (isGpsSubscribed())
            unsubscribeGps()
        else
            subscribeGps()
    }

    fun onClickNewPlantFab() {
        if (!isGpsEnabled()) {
            _gpsFabIcon.value = GPS_OFF.drawable
            showGpsRequiredSnackbar.call()
        } else
            navigateToNewPlant.call()
    }

    fun isGpsSubscribed(): Boolean = locationService.isGpsSubscribed(this)

    fun unsubscribeGps() {
        if (isGpsSubscribed()) {
            _gpsFabIcon.postValue(GPS_OFF.drawable)
            locationService.unsubscribe(this)
        }
        staleGpsTimer?.run { cancel(); staleGpsTimer = null }
    }

    private fun subscribeGps() {
        if (!isGpsSubscribed()) {
            _gpsFabIcon.value = GPS_NO_FIX.drawable
            locationService.subscribe(this, ::onLocation, 5000)
        }
    }

    private fun isGpsEnabled(): Boolean = locationService.isGpsEnabled()

    private fun onLocation(location: Location) {
        _currentLocation.postValue(location)
    }

    fun setStaleGpsLocationTimer() {
        staleGpsTimer?.cancel()
        staleGpsTimer = Timer().schedule(staleGpsTimeout) {
            Lg.d("staleGpsTimer finished.")
            staleGpsTimer = null
            _gpsFabIcon.postValue(GPS_NO_FIX.drawable)
        }
    }

//    private fun getLastLocation(): Location? = locationService.getLastLocation()

    private fun extractPlantMarkerDataList(plantComposites: List<PlantComposite>): List<PlantMarkerData> =
            plantComposites.map { extractPlantMarkerData(it) }

    private fun extractPlantMarkerData(plantComposite: PlantComposite): PlantMarkerData {
        val plant = plantComposite.plant
        val photoPath = plantComposite.plantPhotos.first().fileName
        val location = plantComposite.plantLocations.first().location
        val plantIcon = getPlantIconFromType(plant.type)
        return PlantMarkerData(
            plant.id,
            plant.commonName,
            plant.scientificName,
            location.latitude,
            location.longitude,
            photoPath,
            plant.timestamp.toString().substringBefore('T'),
            plantIcon
        )
    }

    fun getPlantIconFromType(plantType: Plant.Type): Int {
        return when (plantType) {
            TREE -> R.drawable.marker_purple
            SHRUB -> R.drawable.marker_blue
            HERB -> R.drawable.marker_green
            GRASS -> R.drawable.marker_light_green
            VINE -> R.drawable.marker_yellow
            FUNGUS -> R.drawable.marker_yellow // TODO: Use different color
        }
    }
}