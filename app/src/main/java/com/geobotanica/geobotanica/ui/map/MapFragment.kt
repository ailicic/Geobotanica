package com.geobotanica.geobotanica.ui.map

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity.BOTTOM
import android.view.Gravity.CENTER_HORIZONTAL
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.User
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.InternalRenderTheme
import org.mapsforge.map.scalebar.MapScaleBar
import javax.inject.Inject

// TODO: Determine which fragment to load initially instead of forwarding. Maybe use SharedPrefs?
// TODO: Check behaviour in PlantConfirmFragment if toolbar back is pressed (looks like it ignores back button override)
    // NEED activity.toolbar.setNavigationOnClickListener

// LONG TERM
// TODO: Use Okio everywhere
// TODO: Check that coroutine result is handled properly in dialog where user taps outside to close (no result given to getStatus)
// TODO: Check for memory leaks. Is coroutine holding on to Warning Dialog?
// TODO: Login screen
// TODO: Try to replace more callbacks with coroutines where sensible
// https://developer.android.com/training/id-auth/identify.html
// https://developer.android.com/training/id-auth/custom_auth
// TODO: Investigate why app start time is so long (should be less of an issue after login/download screen)
// TODO: Add photoType + editPhoto buttons in PlantDetails image (like confirm frag)
// TODO: Maybe use existing bundle when navigating (it works, but need to be careful about updating old values).
// TODO: Group nearby markers into clusters
// TODO: Limit max height to recyclerview in SearchPlantName (extends below screen)
// TODO: Use MediaStore to store photos. They should apppear in Gallery as an album.
// TODO: Make custom camera screen so Espresso can be used for UI testing (New CameraX API)
// TODO: Use interfaces instead of concrete classes for injected dependencies where appropriate
// TODO: Implement dark theme
// TODO: Try using object detection for assisted plant measurements
// TODO: Show warning dialog to user on zooming out far. Rendering vector maps at low zoom is slow but is only required once for caching.

// LONG TERM NIT PICK
// TODO: Get rid of warning on using null as root layout in inflate calls in onCreateDialog()
// TODO: Learn how to use only the keyboard
// TODO: Check that no hard-coded strings are used -> resources.getString(R.string.trunk_diameter)
// TODO: Use code reformatter:
// Check tabs on fn params / data class
// Subclass in class declaration: colon needs space on both sides
// TODO: Use vector graphics for all icons where possible
// TODO: Decide on Lg.v/d/i etc.
// TODO: Double check proper placement of methods in lifecycle callbacks
    // https://developer.android.com/guide/components/activities/activity-lifecycle
// https://github.com/osmdroid/osmdroid/wiki/Offline-Map-Tiles
// TODO: Fix screen rotation crashes and consider landscape layouts
// TODO: Figure out how to resume app state after onStop, then process death (e.g. home or switch app, then kill app)
// https://medium.com/google-developers/viewmodels-persistence-onsaveinstancestate-restoring-ui-state-and-loaders-fc7cc4a6c090
// TODO: Expose Location events as LiveData in LocationService
// TODO: Consider allowing app to be installed on external storage

// DEFERRED
// TODO: Show PlantType icon in map bubble (and PlantDetail?)


class MapFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<MapViewModel>
    @Inject lateinit var fileDownloader: FileDownloader

    private lateinit var viewModel: MapViewModel

    private lateinit var tileRendererLayer: TileRendererLayer
    private lateinit var tileCache: TileCache
    private val loadedMaps = mutableListOf<String>()

//    private var locationMarker: Marker? = null
//    private var locationPrecisionCircle: Polygon? = null

    private val sharedPrefsIsFirstRun = "isFirstRun"
    private val sharedPrefsMapLatitude = "mapLatitude"
    private val sharedPrefsMapLongitude = "mapLongitude"
    private val sharedPrefsMapZoomLevel = "MapZoomLevel"
    private val sharedPrefsWasGpsSubscribed = "gpsUpdatesSubscribed"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        lifecycleScope.launch(Dispatchers.IO) {
//            GbDatabase.getInstance(appContext).clearAllTables()
            GbDatabase.getInstance(appContext).userDao().insert(User(1, "Guest")) // TODO: Move to Login Screen
        }
        viewModel = getViewModel(viewModelFactory) {
            userId = 1L // TODO: Retrieve userId from LoginFragment Navigation bundle (or shared prefs)
        }
        loadSharedPrefsToViewModel()

        //load/initialize the osmdroid configuration (should be before layout inflation)
//        Configuration.getInstance().load(context, defaultSharedPrefs)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAfterPermissionsGranted()
    }

    private fun initAfterPermissionsGranted() {
        if (! wasPermissionGranted(ACCESS_FINE_LOCATION))
            requestPermission(ACCESS_FINE_LOCATION)
        else
            init()
    }

    override fun onStop() {
        super.onStop()

        // TODO: Remove this conditional after Login screen (permission check will happen there)
        if (wasPermissionGranted(ACCESS_FINE_LOCATION)) // Conditional required for first boot if GPS permission denied
            saveMapStateToViewModel()
        saveSharedPrefsFromViewModel()
        viewModel.unsubscribeGps()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.destroyAll()
    }

    private fun loadSharedPrefsToViewModel() {
        sharedPrefs.let { sp ->
            viewModel.let { vm ->
                vm.isFirstRun = sp.get(sharedPrefsIsFirstRun, vm.isFirstRun)
                vm.mapLatitude = sp.get(sharedPrefsMapLatitude, vm.mapLatitude)
                vm.mapLongitude = sp.get(sharedPrefsMapLongitude, vm.mapLongitude)
                vm.mapZoomLevel = sp.get(sharedPrefsMapZoomLevel, vm.mapZoomLevel)
                vm.wasGpsSubscribed = sp.get(sharedPrefsWasGpsSubscribed, vm.wasGpsSubscribed)
            }
        }
    }

    private fun saveSharedPrefsFromViewModel() {
        sharedPrefs.put(
            sharedPrefsIsFirstRun to false,
            sharedPrefsMapLatitude to viewModel.mapLatitude,
            sharedPrefsMapLongitude to viewModel.mapLongitude,
            sharedPrefsMapZoomLevel to viewModel.mapZoomLevel,
            sharedPrefsWasGpsSubscribed to viewModel.wasGpsSubscribed
        )
    }

    private fun saveMapStateToViewModel() {
//        viewModel.mapZoomLevel = map.zoomLevelDouble
//        viewModel.mapLatitude = map.mapCenter.latitude
//        viewModel.mapLongitude = map.mapCenter.longitude
        viewModel.wasGpsSubscribed = viewModel.isGpsSubscribed()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            getRequestCode(ACCESS_FINE_LOCATION) -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Lg.i("permission.ACCESS_FINE_LOCATION: PERMISSION_GRANTED")
                    initAfterPermissionsGranted()
                } else {
                    Lg.i("permission.ACCESS_FINE_LOCATION: PERMISSION_DENIED")
                    showToast("GPS permission required") // TODO: Find better UX approach (separate screen)
                    activity.finish()
                }
            }
            else -> { }
        }
    }

    private fun init() {
//        // TODO: REMOVE
//        NavHostFragment.findNavController(this).navigate(
//                R.id.downloadTaxaFragment, createBundle() )

        initMap()
        viewModel.initGpsSubscribe()
        setClickListeners()
        bindViewModel()
    }

    private fun initMap() = lifecycleScope.launch {
        mapView.isClickable = true
        mapView.mapScaleBar.isVisible = true
        mapView.mapScaleBar.scaleBarPosition = MapScaleBar.ScaleBarPosition.TOP_LEFT
        mapView.setBuiltInZoomControls(true)

        tileCache = AndroidUtil.createTileCache(context, "mapcache",
                mapView.model.displayModel.tileSize, 1f, mapView.model.frameBufferModel.overdrawFactor, true)
        Lg.v("tileCache: capacity = ${tileCache.capacity}, capacityFirstLevel = ${tileCache.capacityFirstLevel}")
        tileRendererLayer = TileRendererLayer(tileCache, createMultiMapDataStore(),
                mapView.model.mapViewPosition, AndroidGraphicFactory.INSTANCE)
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.DEFAULT)

        mapView.layerManager.layers.add(tileRendererLayer)
        mapView.mapZoomControls.zoomControlsGravity = BOTTOM or CENTER_HORIZONTAL

        mapView.setCenter(LatLong(viewModel.mapLatitude, viewModel.mapLongitude))
        mapView.setZoomLevel(12.toByte())

        coordinatorLayout.isVisible = true // TODO: Remove this after LoginScreen implemented
//        locationMarker = null
//        createLocationPrecisionCircle() // Add to map now to ensure always on bottom

//        if (viewModel.isFirstRun)
//            viewModel.getLastLocation()?.let { centerMapOnLocation(it, false) } // TODO: CHECK IF THIS SHOULD STAY (never worked)


        registerMapDownloadObserver()
    }

    private fun registerMapDownloadObserver() {
        activity.downloadComplete.observe(activity, Observer { downloadId ->
            if (fileDownloader.isMap(downloadId) && ! loadedMaps.contains(fileDownloader.filenameFrom(downloadId)))
                reloadMaps()
        })
    }

    private suspend fun createMultiMapDataStore(): MultiMapDataStore {
        val multiMapDataStore = MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL)

        loadedMaps.clear()
        viewModel.getDownloadedMapFileList().forEach { mapFile ->
            Lg.d("Loading downloaded map: ${mapFile.name}")
            loadedMaps.add(mapFile.name)
            multiMapDataStore.addMapDataStore(MapFile(mapFile), false, false)
        }
        return multiMapDataStore
    }

    private fun reloadMaps() = lifecycleScope.launch {
        Lg.d("MapFragment: reloadMaps()")
        mapView.layerManager.layers.remove(tileRendererLayer)
        tileRendererLayer.onDestroy()
        tileCache.purge() // Delete all cache files. If omitted, existing cache will override new maps.
        tileRendererLayer = TileRendererLayer(tileCache, createMultiMapDataStore(),
                mapView.model.mapViewPosition, AndroidGraphicFactory.INSTANCE)
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.DEFAULT)
        mapView.layerManager.layers.add(tileRendererLayer)
        mapView.layerManager.redrawLayers()
    }

    @Suppress("DEPRECATION")
    private fun setClickListeners() {
        gpsFab.setOnClickListener { viewModel.onClickGpsFab() }
        fab.setOnClickListener { viewModel.onClickNewPlantFab() }
    }

    private fun bindViewModel() {
        with(viewModel) {
            gpsFabIcon.observe(viewLifecycleOwner,
                Observer { @Suppress("DEPRECATION")
                    gpsFab.setImageDrawable(resources.getDrawable(it))
                }
            )
            showGpsRequiredSnackbar.observe(viewLifecycleOwner, onGpsRequiredSnackbar)
            navigateToNewPlant.observe(viewLifecycleOwner, onNavigateToNewPlant)
//            plantMarkerData.observe(viewLifecycleOwner, onPlantMarkers)
//            currentLocation.observe(viewLifecycleOwner, onLocation)
        }
    }

    private val onGpsRequiredSnackbar = Observer<Unit> {
        showSnackbar(R.string.gps_must_be_enabled, R.string.enable) {
            startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private val onNavigateToNewPlant = Observer<Unit> {
        NavHostFragment.findNavController(this).navigate(
                R.id.newPlantPhotoFragment,
                bundleOf("userId" to viewModel.userId) )
    }

//     TODO: REMOVE (Temp for NewPlantConfirmFragment)
//    private val onNavigateToNewPlant = Observer<Unit> {
//        NavHostFragment.findNavController(this).navigate(R.id.newPlantConfirmFragment, createBundle() )
//    }

    // TODO: REMOVE
    private fun createBundle(): Bundle {
        return bundleOf(
                userIdKey to viewModel.userId,
                plantTypeKey to Plant.Type.TREE.flag,
                photoUriKey to fileFromDrawable(R.drawable.photo_type_complete, "photo_type_complete"),
                commonNameKey to "Common",
                scientificNameKey to "Latin",
                heightMeasurementKey to Measurement(1.0f, Units.M),
                diameterMeasurementKey to Measurement(2.0f, Units.IN),
                trunkDiameterMeasurementKey to Measurement(3.5f, Units.FT)
        ).apply {
            putSerializable(locationKey, Location(
                49.477, -119.592, 1.0, 3.0f, 10, 20))
        }
    }

//    private val onPlantMarkers = Observer< List<PlantMarkerData> > { newPlantMarkersData ->
//        val currentGbMarkers = map.overlays.filterIsInstance<GbMarker>()
//        val plantMarkerDiffs = computeDiffs(
//                currentGbMarkers.map { it.plantId }, newPlantMarkersData.map { it.plantId }
//        )
//
//        map.overlays.removeAll( // Must be before add (for updated markers)
//                currentGbMarkers.filter { plantMarkerDiffs.removeIds.contains(it.plantId) } )
//        map.overlays.addAll( // Must be after remove (for updated markers)
//                newPlantMarkersData
//                        .filter { plantMarkerDiffs.insertIds.contains(it.plantId) }
//                        .map { GbMarker(it, activity, map) } )
//        // TODO: Consider using a custom InfoWindow
//        // https://code.google.com/archive/p/osmbonuspack/wikis/Tutorial_2.wiki
//        // 7. Customizing the bubble behaviour:
//        // 9. Creating your own bubble layout
//
//        forceLocationMarkerOnTop()
//        map.invalidate()
//    }

//    private fun forceLocationMarkerOnTop() {
//        locationMarker?.let {
//            map.overlays.remove(it)
//            map.overlays.add(it)
//        }
//    }

    // TODO: See if logic here can be pushed to VM
//    @Suppress("DEPRECATION")
//    private val onLocation = Observer<Location> {
////        Lg.v("MapFragment: onLocation() = $it")
//        if (it.latitude != null && it.longitude != null) { // OK in VM
//            if (it.isRecent()) { // OK in VM
//                gpsFab.setImageDrawable(resources.getDrawable(R.drawable.gps_fix)) // Could be LiveData
//                updateLocationPrecision(it) // Could be LiveData
//                if (isLocationOffScreen(it)) // Problem: Check requires map
//                    centerMapOnLocation(it) // Could be LiveData
//                viewModel.setStaleGpsLocationTimer() // OK in VM
//            }
//            updateLocationMarker(it) // Could be LiveData
//            map.invalidate() // Could be LiveData
//        }
//        // TODO: Update satellitesInUse
//    }

//    private fun updateLocationPrecision(location: Location) {
//        if (locationPrecisionCircle == null)
//            createLocationPrecisionCircle()
//        locationPrecisionCircle?.points = Polygon.pointsAsCircle(
//            GeoPoint(location.latitude!!, location.longitude!!),
//            location.precision!!.toDouble()
//        )
//    }

//    @Suppress("DEPRECATION")
//    private fun createLocationPrecisionCircle() {
//        locationPrecisionCircle = Polygon(map).apply {
//            fillColor = 0x12121212
//            strokeColor = resources.getColor(R.color.colorPrimaryDark)
//            strokeWidth = 3F
//            infoWindow = null
//        }
//        map.overlays.add(locationPrecisionCircle)
//    }

//    private fun updateLocationMarker(currentLocation: Location) {
//        if (locationMarker == null)
//            createLocationMarker()
//        locationMarker?.position?.setCoords(currentLocation.latitude!!, currentLocation.longitude!!)
//    }

//    @Suppress("DEPRECATION")
//    private fun createLocationMarker() {
//        locationMarker = Marker(map).apply {
//            icon = resources.getDrawable(R.drawable.person)
//            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
//            setOnMarkerClickListener { _, _ ->
//                showToast("You are here")
//                true
//            }
//            map.overlays.add(this)
//        }
//    }
//
//    private fun isLocationOffScreen(location: Location): Boolean =
//        !map.projection.boundingBox.contains(location.latitude!!, location.longitude!!)
//
//    private fun centerMapOnLocation(location: Location, animate: Boolean = true) {
//        if (animate)
//            location.let { map.controller.animateTo( GeoPoint(it.latitude!!, it.longitude!!) ) }
//        else
//            location.let { map.controller.setCenter( GeoPoint(it.latitude!!, it.longitude!!) ) }
//    }
}