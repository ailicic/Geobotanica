package com.geobotanica.geobotanica.ui.map

import androidx.lifecycle.*
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.ui.map.MapViewModel.GpsFabDrawable.GPS_NO_FIX
import com.geobotanica.geobotanica.ui.map.MapViewModel.GpsFabDrawable.GPS_OFF
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SingleLiveEvent
import com.geobotanica.geobotanica.util.schedule
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class MapViewModel @Inject constructor(
    private val storageHelper: StorageHelper,
    private val mapRepo: MapRepo,
    private val assetRepo: AssetRepo,
    private val plantRepo: PlantRepo,
    private val locationService: LocationService
): ViewModel() {
    var userId = 0L    // Field injected dynamic parameter

    val plantMarkerData: LiveData< List<PlantMarkerData> > by lazy {
        plantRepo.getAllPlantComposites().map { extractPlantMarkerDataList(it) }
    }

    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> = _currentLocation

    private val _gpsFabIcon = MutableLiveData<Int>()
    val gpsFabIcon: LiveData<Int> = _gpsFabIcon

    val showGpsRequiredSnackbar = SingleLiveEvent<Unit>()
    val showPlantNamesMissingSnackbar = SingleLiveEvent<Unit>()
    val navigateToNewPlant = SingleLiveEvent<Unit>()

    private val defaultMapZoomLevel = 16
    private val defaultMapLatitude = 49.477
    private val defaultMapLongitude = -119.59

    var mapZoomLevel: Int = defaultMapZoomLevel
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

    suspend fun getDownloadedMapFileList(): List<File> {
        val mapsPath = storageHelper.getMapsPath()
        return mutableListOf(
                File(mapsPath, assetRepo.get(OnlineAssetId.WORLD_MAP.id).filename)
        ).apply {
            mapRepo.getDownloaded().forEach { map ->
                add(File(mapsPath, map.filename))
            }
        }
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

    fun onClickNewPlantFab() = viewModelScope.launch {
        if (!isGpsEnabled()) {
            _gpsFabIcon.value = GPS_OFF.drawable
            showGpsRequiredSnackbar.call()
        } else if (! assetRepo.get(OnlineAssetId.PLANT_NAMES.id).isDownloaded) {
            showPlantNamesMissingSnackbar.call()
        } else
            navigateToNewPlant.call()
    }

    fun isGpsSubscribed(): Boolean = locationService.isGpsSubscribed(this)

    fun unsubscribeGps() {
        if (isGpsSubscribed()) {
            _gpsFabIcon.value = GPS_OFF.drawable
            locationService.unsubscribe(this)
        }
        staleGpsTimer?.run { cancel(); staleGpsTimer = null }
    }

    fun setStaleGpsLocationTimer() {
        staleGpsTimer?.cancel()
        staleGpsTimer = Timer().schedule(staleGpsTimeout) {
            Lg.d("staleGpsTimer finished.")
            staleGpsTimer = null
            _gpsFabIcon.value = GPS_NO_FIX.drawable
        }
    }

    private fun subscribeGps() {
        if (!isGpsSubscribed()) {
            _gpsFabIcon.value = GPS_NO_FIX.drawable
            locationService.subscribe(this, ::onLocation, 5000)
        }
    }

    private fun isGpsEnabled(): Boolean = locationService.isGpsEnabled()

    private fun onLocation(location: Location) {
        _currentLocation.value = location
    }

//    private fun getLastLocation(): Location? = locationService.getLastLocation()

    private fun extractPlantMarkerDataList(plantComposites: List<PlantComposite>): List<PlantMarkerData> =
            plantComposites.map { extractPlantMarkerData(it) }

    private fun extractPlantMarkerData(plantComposite: PlantComposite): PlantMarkerData {
        val plant = plantComposite.plant
        val photoFilename = plantComposite.plantPhotos
                .filter { it.type == PlantPhoto.Type.COMPLETE }
                .maxBy { it.timestamp }
                ?.filename
        val location = plantComposite.plantLocations
                .maxBy { it.location.timestamp }
                ?.location!!

        return PlantMarkerData(
            plant.id,
            plantComposite.plant.type,
            plant.commonName,
            plant.scientificName,
            location.latitude,
            location.longitude,
            photoFilename,
            plant.timestamp.toString().substringBefore('T')
        )
    }
}

data class PlantMarkerData(
        val plantId: Long,
        val plantType: Plant.Type? = null,
        val commonName: String? = null,
        val scientificName: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val photoFilename: String? = null,
        val dateCreated: String? = null
)