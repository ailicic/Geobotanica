package com.geobotanica.geobotanica.ui.map

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.location.Location
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.User
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.map.marker.LocationCircle
import com.geobotanica.geobotanica.ui.map.marker.LocationMarker
import com.geobotanica.geobotanica.ui.map.marker.PlantMarker
import com.geobotanica.geobotanica.ui.map.marker.PlantMarkerData
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.Measurement
import com.geobotanica.geobotanica.util.get
import com.geobotanica.geobotanica.util.put
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: Remove unnecessary usage of beforeEachBlockingTest { }
// TODO: Use relaxed mocks sparingly (slow to generate) (create mockObserver() )
// TODO: Define PlantPhoto.Type flags and simplify DAO accordingly
// TODO: Add more tests
// TODO: Login screen (no authentication, just select user with any pw)
// TODO: Request all permissions in separate screen before map (prob after login/splash screen for ux)
// TODO: Setup CI (Bitrise)

// LONG TERM
// TODO: Use Okio everywhere
// https://developer.android.com/training/id-auth/identify.html
// https://developer.android.com/training/id-auth/custom_auth
// TODO: Group nearby markers into clusters
// TODO: Use MediaStore to store photos. They should appear in Gallery as an album.
// TODO: Make custom camera screen so Espresso can be used for UI testing (New CameraX API)
// TODO: Implement dark theme
// TODO: Try using object detection for assisted plant measurements
// TODO: Allow user to select default units in preferences
// TODO: Consider adding another screen after plant name to select infraspecific epithet (currently dupes for generic+epithet exist due to infras)
// TODO: Consider allowing plant name searching when editing name (likely going to be messy)
// TODO: MAYBE Handle nullifying taxon/vernacular id if plant name is modified in PlantDetailFragment (related to above)

// LONG TERM NIT PICK
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
// TODO: Consider allowing app to be installed on external storage


class MapFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<MapViewModel>
    @Inject lateinit var fileDownloader: FileDownloader

    private lateinit var viewModel: MapViewModel

    private var locationMarker: LocationMarker? = null
    private var locationPrecisionCircle: LocationCircle? = null

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
            userId = 1L // TODO: Retrieve userId from LoginFragment Navigation bundle
        }
        loadSharedPrefsToViewModel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_map, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                showToast("Settings")
                true
            }
            R.id.download_maps -> {
                mapView.mapScaleBar.isVisible = false // Prevents crash on fragment anim
                navigateTo(R.id.action_map_to_localMaps)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    override fun onStart() {
        super.onStart()
        viewModel.initGpsSubscribe()
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
                vm.mapLatitude = sp.get(sharedPrefsMapLatitude, vm.mapLatitude)
                vm.mapLongitude = sp.get(sharedPrefsMapLongitude, vm.mapLongitude)
                vm.mapZoomLevel = sp.get(sharedPrefsMapZoomLevel, vm.mapZoomLevel)
                vm.wasGpsSubscribed = sp.get(sharedPrefsWasGpsSubscribed, vm.wasGpsSubscribed)
            }
        }
    }

    private fun saveSharedPrefsFromViewModel() {
        sharedPrefs.put(
            sharedPrefsMapLatitude to viewModel.mapLatitude,
            sharedPrefsMapLongitude to viewModel.mapLongitude,
            sharedPrefsMapZoomLevel to viewModel.mapZoomLevel,
            sharedPrefsWasGpsSubscribed to viewModel.wasGpsSubscribed
        )
    }

    private fun saveMapStateToViewModel() {
        viewModel.mapZoomLevel = mapView.model.mapViewPosition.zoomLevel.toInt()
        viewModel.mapLatitude = mapView.model.mapViewPosition.center.latitude
        viewModel.mapLongitude = mapView.model.mapViewPosition.center.longitude
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

    private fun init() = lifecycleScope.launch {
        defaultSharedPrefs.put(sharedPrefsIsFirstRunKey to false)
        viewModel.wasGpsSubscribed = true // Enable GPS by default (GPS permission available now)
        viewModel.initGpsSubscribe()
        initMap()
        setClickListeners()
        bindViewModel()
    }

    private suspend fun initMap() {
        coordinatorLayout.isVisible = true // TODO: Remove this after LoginScreen implemented
        locationPrecisionCircle = null
        locationMarker = null

        with(viewModel) {
            mapView.init(getDownloadedMapFileList(), mapLatitude, mapLongitude, mapZoomLevel)
        }
        registerMapDownloadObserver()
        mapView.reloadMarkers.observe(viewLifecycleOwner, Observer { reloadMarkers() })

//        if (viewModel.isFirstRun)
//            viewModel.getLastLocation()?.let { centerMap(it, false) } // TODO: CHECK IF THIS SHOULD STAY (never worked)
    }

    private fun registerMapDownloadObserver() {
        activity.downloadComplete.observe(viewLifecycleOwner, Observer { downloadId ->
            if (fileDownloader.isMap(downloadId) && ! mapView.isMapLoaded(fileDownloader.filenameFrom(downloadId))) {
                lifecycleScope.launch {
                    mapView.reloadMaps(viewModel.getDownloadedMapFileList())
                }
            }
        })
    }

    private fun reloadMarkers() {
        Lg.d("reloadMarkers()")
        viewModel.plantMarkerData.removeObserver(onPlantMarkers)
        viewModel.plantMarkerData.observe(viewLifecycleOwner, onPlantMarkers)
    }

    private fun setClickListeners() {
        gpsFab.setOnClickListener { viewModel.onClickGpsFab() }
        fab.setOnClickListener { viewModel.onClickNewPlantFab() }
    }

    private fun bindViewModel() {
        with(viewModel) {
            gpsFabIcon.observe(viewLifecycleOwner, onGpsFabIcon)
            showGpsRequiredSnackbar.observe(viewLifecycleOwner, onGpsRequiredSnackbar)
            showPlantNamesMissingSnackbar.observe(viewLifecycleOwner, onPlantNamesMissingSnackbar)
            updateLocationPrecisionMarker.observe(viewLifecycleOwner, onUpdateLocationPrecision)
            updateLocationMarker.observe(viewLifecycleOwner, onUpdateLocationMarker)
            centerMap.observe(viewLifecycleOwner, onEnsureMapBoundsIncludeLocation)
            redrawMapLayers.observe(viewLifecycleOwner, onRedrawMapLayers)
            navigateToNewPlant.observe(viewLifecycleOwner, onNavigateToNewPlant)
            plantMarkerData.observe(viewLifecycleOwner, onPlantMarkers)
        }
    }

    private val onGpsFabIcon = Observer<Int> { gpsFab.setImageDrawable(resources.getDrawable(it, appContext.theme)) }

    private val onGpsRequiredSnackbar = Observer<Unit> {
        showSnackbar(R.string.gps_must_be_enabled, R.string.enable) {
            startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private val onPlantNamesMissingSnackbar = Observer<Unit> { showSnackbar(R.string.wait_for_plant_name_database_to_download) }

    private val onUpdateLocationPrecision = Observer<Location> { location ->
        if (locationPrecisionCircle == null) {
            locationPrecisionCircle = LocationCircle(mapView)
            forceLocationMarkerOnTop()
        }
        locationPrecisionCircle?.updateLocation(location)
    }

    private val onUpdateLocationMarker = Observer<Location> { location ->
        locationMarker ?: createLocationMarker()
        locationMarker?.updateLocation(location)
    }

    private val onEnsureMapBoundsIncludeLocation = Observer<Location> { mapView.centerMap(it) }

    private val onRedrawMapLayers = Observer<Unit> { mapView.layerManager.redrawLayers() }

    private val onNavigateToNewPlant = Observer<Unit> {
        mapView.mapScaleBar.isVisible = false // Prevents crash on fragment anim
        navigateTo(
                R.id.action_map_to_newPlantPhoto,
                bundleOf(
                        userIdKey to viewModel.userId,
                        newPlantSessionIdKey to System.currentTimeMillis()
                )
        )
//        navigateTo(
//                R.id.newPlantConfirmFragment,
//                bundleOf(
//                        userIdKey to viewModel.userId,
//                        photoUriKey to fileFromDrawable(R.drawable.plant_type_tree, "tree"),
//                        commonNameKey to "Common",
//                        scientificNameKey to "Scientific",
//                        plantTypeKey to Plant.Type.TREE.flag,
//                        heightMeasurementKey to Measurement(1.0f),
//                        diameterMeasurementKey to Measurement(2.0f),
//                        trunkDiameterMeasurementKey to Measurement(3.0f)
//                )
//        )
    }

    private val onPlantMarkers = Observer< List<PlantMarkerData> > { newPlantMarkerData ->
        Lg.d("onPlantMarkers: $newPlantMarkerData")

        val currentPlantMarkers = mapView.layerManager.layers.filterIsInstance<PlantMarker>()

        val plantMarkerDiffs = viewModel.getPlantMarkerDiffs(
                currentPlantMarkers.map { it.plantMarkerData },
                newPlantMarkerData
        )

        val removeIds = plantMarkerDiffs.toRemove.map { it.plantId }
        currentPlantMarkers
                .filter { removeIds.contains(it.plantId) }
                .forEach { mapView.layerManager.layers.remove(it, false) } // Note: removeAll fails due to COW array internal to layers

        mapView.layerManager.layers.addAll(
                plantMarkerDiffs.toInsert.map { PlantMarker(it, activity, mapView, ::onPlantMarkerLongPress) },
                false
        )

        forceLocationMarkerOnTop()
        mapView.layerManager.redrawLayers()
    }

    private fun onPlantMarkerLongPress(plantId: Long) {
        Lg.d("Opening plant detail: id=plantId")
        mapView.mapScaleBar.isVisible = false // Prevents crash on fragment anim
        val bundle = bundleOf(plantIdKey to plantId, userIdKey to viewModel.userId)
        navigateTo(R.id.action_map_to_plantDetail, bundle)
    }


    private fun createLocationMarker() {
        locationMarker = LocationMarker(resources.getDrawable(R.drawable.person, null), mapView)
        locationMarker?.showLocationMarkerToast?.observe(viewLifecycleOwner, Observer {
            showToast(R.string.you_are_here)
        })
    }

    private fun forceLocationMarkerOnTop() {
        locationPrecisionCircle?.let {
            mapView.layerManager.layers.remove(it, false)
            mapView.layerManager.layers.add(it, false)
        }
        locationMarker?.let {
            mapView.layerManager.layers.remove(it, false)
            mapView.layerManager.layers.add(it, false)
        }
    }
}

