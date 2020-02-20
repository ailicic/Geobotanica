package com.geobotanica.geobotanica.ui.map

import androidx.lifecycle.*
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.location.Location
import com.geobotanica.geobotanica.android.location.LocationSubscriber
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.ui.login.OnlineAssetId.*
import com.geobotanica.geobotanica.ui.map.MapViewModel.GpsFabDrawable.*
import com.geobotanica.geobotanica.ui.map.marker.PlantMarkerData
import com.geobotanica.geobotanica.ui.map.marker.PlanterMarkerDiffer
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
    private val plantMarkerDiffer: PlanterMarkerDiffer,
    private val locationService: LocationService
): ViewModel(), LocationSubscriber {
    var userId = 0L    // Field injected dynamic parameter

    val plantMarkerData: LiveData< List<PlantMarkerData> > by lazy {
        plantRepo.getAllPlantComposites().map { plantCompositeList ->
            plantCompositeList.map { plantComposite -> PlantMarkerData(plantComposite) }
        }
    }

    private val _gpsFabIcon = MutableLiveData<Int>()
    val gpsFabIcon: LiveData<Int> = _gpsFabIcon

    private val _updateLocationPrecisionMarker = MutableLiveData<Location>()
    val updateLocationPrecisionMarker: LiveData<Location> = _updateLocationPrecisionMarker

    private val _updateLocationMarker = MutableLiveData<Location>()
    val updateLocationMarker: LiveData<Location> = _updateLocationMarker

    val showGpsRequiredSnackbar = SingleLiveEvent<Unit>()
    val showPlantNamesMissingSnackbar = SingleLiveEvent<Unit>()
    val navigateToNewPlant = SingleLiveEvent<Unit>()
    val centerMap = SingleLiveEvent<Location>()
    val redrawMapLayers = SingleLiveEvent<Unit>()

    private val defaultMapZoomLevel = 16
    private val defaultMapLatitude = 49.477
    private val defaultMapLongitude = -119.59

    var mapZoomLevel: Int = defaultMapZoomLevel
    var mapLatitude: Double = defaultMapLatitude
    var mapLongitude: Double = defaultMapLongitude
    var wasGpsSubscribed = false

    private var staleGpsTimer: Timer? = null
    private val staleGpsTimeout = 120_000L // ms

    enum class GpsFabDrawable(val drawable: Int) {
        GPS_OFF(R.drawable.gps_off),
        GPS_NO_FIX(R.drawable.gps_no_fix),
        GPS_FIX(R.drawable.gps_fix)
    }

    override fun onLocation(location: Location) {
        if (location.latitude != null && location.longitude != null) {
            if (location.isRecent()) {
                _gpsFabIcon.value = GPS_FIX.drawable
                _updateLocationPrecisionMarker.value = location
                centerMap.value = location
                setStaleGpsLocationTimer()
            }
            _updateLocationMarker.value = location
            redrawMapLayers.call()
        }
    }

    suspend fun getDownloadedMapFileList(): List<File> {
        val mapsPath = storageHelper.getMapsPath()
        return mutableListOf(
                File(mapsPath, assetRepo.get(WORLD_MAP.id).filenameUngzip)
        ).apply {
            mapRepo.getDownloaded().forEach { map ->
                add(File(mapsPath, map.filename))
            }
        }
    }

    fun initGpsSubscribe() {
        if (!isGpsEnabled())
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
        } else if (! assetRepo.get(PLANT_NAMES.id).isDownloaded) {
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

    fun getPlantMarkerDiffs(currentData: List<PlantMarkerData>, newData: List<PlantMarkerData>) =
        plantMarkerDiffer.getDiffs(currentData, newData)


    private fun subscribeGps() {
        if (!isGpsSubscribed()) {
            _gpsFabIcon.value = GPS_NO_FIX.drawable
            locationService.subscribe(this, 5000)
        }
    }

    private fun isGpsEnabled(): Boolean = locationService.isGpsEnabled()

    private fun setStaleGpsLocationTimer() {
        staleGpsTimer?.cancel()
        staleGpsTimer = Timer().schedule(staleGpsTimeout) {
            Lg.d("staleGpsTimer finished.")
            staleGpsTimer = null
            _gpsFabIcon.value = GPS_NO_FIX.drawable
        }
    }

//    private fun getLastLocation(): Location? = locationService.getLastLocation()
}
