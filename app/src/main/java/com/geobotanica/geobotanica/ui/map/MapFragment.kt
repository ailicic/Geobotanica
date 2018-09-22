package com.geobotanica.geobotanica.ui.map

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.User
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.map.MapViewModel.GpsFabState.*
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SharedPrefsExt.get
import com.geobotanica.geobotanica.util.SharedPrefsExt.putSharedPrefs
import com.geobotanica.geobotanica.util.unsubscribeThenObserve
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_map.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import javax.inject.Inject


// TODO: Show snackbar after plant saved (pass as param in Navigate)

// TODO: Show satellite stats too

// TODO: Learn how to use only the keyboard



// LONG TERM
// TODO: Use vector graphics for all icons where possible
// TODO: Decide on Lg.v/d/i etc.
// TODO: Double check proper placement of methods in lifecycle callbacks
    // https://developer.android.com/guide/components/activities/activity-lifecycle
// TODO: Group nearby markers into clusters
// TODO: Create download map activity and utilize offline map tiles
// https://github.com/osmdroid/osmdroid/wiki/Offline-Map-Tiles
// TODO: Fix screen rotation crashes and consider landscape layouts
// TODO: Figure out how to resume app state after onStop, then process death (e.g. home or switch app)
// https://medium.com/google-developers/viewmodels-persistence-onsaveinstancestate-restoring-ui-state-and-loaders-fc7cc4a6c090
// TODO: Check if resume state is handled correctly: start app using AS, press home, kill app using AS, open app




class MapFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<MapViewModel>
    private lateinit var viewModel: MapViewModel

    private var locationMarker: Marker? = null
    private var locationPrecisionCircle: Polygon? = null
    private var staleGpsTimer: CountDownTimer? = null // TODO: Use java.util timer

    private val sharedPrefsIsFirstRun = "isFirstRun"
    private val sharedPrefsMapLatitude = "mapLatitude"
    private val sharedPrefsMapLongitude = "mapLongitude"
    private val sharedPrefsMapZoomLevel = "MapZoomLevel"
    private val sharedPrefsWasGpsSubscribed = "gpsUpdatesSubscribed"


    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        GbDatabase.getInstance(appContext).userDao().insert(User(1, "Guest")) // TODO: Move to Login Screen
        viewModel = getViewModel(viewModelFactory) {
            userId = 1L // TODO: Retrieve userId from LoginFragment Navigation bundle (or shared prefs)
        }
        loadSharedPrefsToViewModel()

        //load/initialize the osmdroid configuration, this can be done
        Configuration.getInstance().load(context, defaultSharedPrefs)
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!wasPermissionGranted(WRITE_EXTERNAL_STORAGE))
            requestPermission(WRITE_EXTERNAL_STORAGE)
        else if (!wasPermissionGranted(ACCESS_FINE_LOCATION))
            requestPermission(ACCESS_FINE_LOCATION)
        else
            init()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onStop() {
        super.onStop()

        // TODO: Delete. Not required after timer in VM (see below)
        staleGpsTimer?.run { cancel(); staleGpsTimer = null }

        // TODO: Remove this conditional after Login screen (permission check will happen there)
        if (wasPermissionGranted(ACCESS_FINE_LOCATION)) // Required for first boot if GPS permission denied
            saveMapStateToViewModel()
        saveSharedPrefsFromViewModel()
        viewModel.unsubscribeGps() // TODO: Cancel timer in this function
    }

    private fun saveSharedPrefsFromViewModel() {
        putSharedPrefs(
            sharedPrefsIsFirstRun to false,
            sharedPrefsMapLatitude to viewModel.mapLatitude,
            sharedPrefsMapLongitude to viewModel.mapLongitude,
            sharedPrefsMapZoomLevel to viewModel.mapZoomLevel,
            sharedPrefsWasGpsSubscribed to viewModel.wasGpsSubscribed
        )
    }

    private fun loadSharedPrefsToViewModel() {
        sharedPrefs.let { sp ->
            viewModel.let {vm ->
                vm.isFirstRun = sp.get(sharedPrefsIsFirstRun, vm.isFirstRun)
                vm.mapLatitude = sp.get(sharedPrefsMapLatitude, vm.mapLatitude)
                vm.mapLongitude = sp.get(sharedPrefsMapLongitude, vm.mapLongitude)
                vm.mapZoomLevel = sp.get(sharedPrefsMapZoomLevel, vm.mapZoomLevel)
                vm.wasGpsSubscribed = sp.get(sharedPrefsWasGpsSubscribed, vm.wasGpsSubscribed)
            }
        }
    }

    private fun saveMapStateToViewModel() {
        viewModel.mapZoomLevel = map.zoomLevelDouble
        viewModel.mapLatitude = map.mapCenter.latitude
        viewModel.mapLongitude = map.mapCenter.longitude
        viewModel.wasGpsSubscribed = viewModel.isGpsSubscribed()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            getRequestCode(WRITE_EXTERNAL_STORAGE) -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Lg.i("onRequestPermissionsResult(): permission.WRITE_EXTERNAL_STORAGE: PERMISSION_GRANTED")
                    if (!wasPermissionGranted(ACCESS_FINE_LOCATION))
                        requestPermission(ACCESS_FINE_LOCATION)
                    else
                        init()
                } else {
                    Lg.i("onRequestPermissionsResult(): permission.WRITE_EXTERNAL_STORAGE: PERMISSION_DENIED")
                    showToast("External storage permission required")
                    activity.finish()
                }
            }
            getRequestCode(ACCESS_FINE_LOCATION) -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Lg.i("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_GRANTED")
                    if (!wasPermissionGranted(WRITE_EXTERNAL_STORAGE))
                        requestPermission(WRITE_EXTERNAL_STORAGE)
                    else
                        init()
                } else {
                    Lg.i("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_DENIED")
                    showToast("GPS permission required")
                    activity.finish()
                }
            }
            else -> { }
        }
    }

    private fun init() {
        initMap()
        viewModel.initGpsSubscribe()
        setClickListeners()
        bindViewModel()
    }

    private fun initMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)

        with(map.controller) {
            setZoom(viewModel.mapZoomLevel)
            setCenter( GeoPoint(viewModel.mapLatitude, viewModel.mapLongitude) )
        }
        coordinatorLayout.visibility = View.VISIBLE // TODO: Remove this after LoginScreen implemented
        locationMarker = null
        createLocationPrecisionCircle()

//        if (viewModel.isFirstRun)
//            viewModel.getLastLocation()?.let { centerMapOnLocation(it, false) } // TODO: CHECK IF THIS SHOULD STAY (never worked)
    }

    @Suppress("DEPRECATION")
    private fun setClickListeners() {
        gpsFab.setOnClickListener { viewModel.onClickGpsFab() }
        newPlantFab.setOnClickListener { viewModel.onClickNewPlantFab() }
    }

    private fun bindViewModel() {
        with(viewModel) {
            gpsFabIcon.unsubscribeThenObserve(this@MapFragment,
                Observer { @Suppress("DEPRECATION")
                    gpsFab.setImageDrawable(resources.getDrawable(it))
                }
            )
            showGpsRequiredSnackbar.unsubscribeThenObserve(this@MapFragment, onGpsRequiredSnackbar)
            navigateToNewPlant.unsubscribeThenObserve(this@MapFragment, onNavigateToNewPlant)
            plantMarkerData.unsubscribeThenObserve(this@MapFragment, onPlantMarkers)
            currentLocation.unsubscribeThenObserve(this@MapFragment, onLocation)
        }
    }

    private val onGpsRequiredSnackbar = Observer<Unit> {
        Snackbar.make(coordinatorLayout, R.string.gps_must_be_enabled, Snackbar.LENGTH_LONG)
            .setAction(R.string.enable) {_ ->
                startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }.show()
    }

    private val onNavigateToNewPlant = Observer<Unit> {
        val bundle = bundleOf("userId" to viewModel.userId)
        val navController = activity.findNavController(R.id.fragment)
        navController.navigate(R.id.newPlantTypeFragment, bundle)
    }

    private val onPlantMarkers = Observer< List<PlantMarkerData> > { newPlantMarkersData ->
        val gbMarkers = map.overlays.filterIsInstance<GbMarker>()
        if (gbMarkers.isEmpty()) { // Trivial case. Add all plant markers to map.
            map.overlays.addAll(newPlantMarkersData.map { GbMarker(it, activity, map) })
        } else { // Compute diffs and apply to existing plant markers.
            val currentIds = gbMarkers.map { it.plantId }
            val newIds = newPlantMarkersData.map { it.plantId }

            val idsToRemove = currentIds subtract newIds
            val idsToInsert = newIds subtract currentIds
            val idsNotChanged = currentIds intersect newIds // TODO: Need a deep comparison to detect updated markers
            Lg.v("idsToRemove=${idsToRemove.count()}")
            Lg.v("idsToInsert: ${idsToInsert.count()}")
            Lg.v("idsNotChanged: ${idsNotChanged.count()}")
            with(map.overlays) {
                addAll(
                    newPlantMarkersData
                        .filter { idsToInsert.contains(it.plantId) }
                        .map { GbMarker(it, activity, map) }
                )
                removeAll(gbMarkers
                        .filter { idsToRemove.contains(it.plantId) })
            }
        }

        // TODO: Consider using a custom InfoWindow
        // https://code.google.com/archive/p/osmbonuspack/wikis/Tutorial_2.wiki
        // 7. Customizing the bubble behaviour:
        // 9. Creating your own bubble layout

        locationMarker?.let {// Force location marker on top of plant markers
            map.overlays.remove(it)
            map.overlays.add(it)
        }
        map.invalidate()
    }

    // TODO: Identify opportunities to move logic into VM
    private val onLocation = Observer<Location> {
        Lg.v("MapFragment: onLocation() = $it")
        if (it.latitude != null && it.longitude != null) {
            if (it.isRecent()) {
                setFabGpsIcon(GPS_FIX)
                updateLocationPrecision(it)
                if (isLocationOffScreen(it))
                    centerMapOnLocation(it)
                setStaleGpsLocationTimer()
            }
            updateLocationMarker(it)
            map.invalidate()
        }
        // TODO: Update satellitesInUse
    }

//    // TODO: Move to VM (all should be fine if FabIcon has LiveData)
    // -> MAYBE CAN USE JAVA TIMER? ->  	java.util.Timer
    private fun setStaleGpsLocationTimer() {
        staleGpsTimer?.run { cancel() }
        staleGpsTimer = object: CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) { }

            override fun onFinish() {
                Lg.d("staleGpsTimer finished.")
                setFabGpsIcon(GPS_NO_FIX)
            }
        }.start()
    }

    private fun updateLocationPrecision(location: Location) {
        if (locationPrecisionCircle == null)
            createLocationPrecisionCircle()
        locationPrecisionCircle?.points = Polygon.pointsAsCircle(
            GeoPoint(location.latitude!!, location.longitude!!),
            location.precision!!.toDouble()
        )
    }

    @Suppress("DEPRECATION")
    private fun createLocationPrecisionCircle() {
        locationPrecisionCircle = Polygon(map).apply {
            fillColor = 0x12121212
            strokeColor = resources.getColor(R.color.colorPrimaryDark)
            strokeWidth = 3F
            infoWindow = null
        }
        map.overlays.add(locationPrecisionCircle)
    }

    private fun updateLocationMarker(currentLocation: Location) {
        Lg.d("updateLocationMarker(): locationMarker=$locationMarker")
        if (locationMarker == null)
            createLocationMarker()
        locationMarker?.position?.setCoords(currentLocation.latitude!!, currentLocation.longitude!!)
    }

    @Suppress("DEPRECATION")
    private fun createLocationMarker() {
        locationMarker = Marker(map).apply {
            icon = resources.getDrawable(R.drawable.person)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setOnMarkerClickListener { _, _ ->
                showToast("You are here")
                true
            }
            map.overlays.add(this)
        }
    }

    private fun isLocationOffScreen(location: Location): Boolean =
        !map.projection.boundingBox.contains(location.latitude!!, location.longitude!!)

    private fun centerMapOnLocation(location: Location, animate: Boolean = true) {
        if (animate)
            location.let { map.controller.animateTo( GeoPoint(it.latitude!!, it.longitude!!) ) }
        else
            location.let { map.controller.setCenter( GeoPoint(it.latitude!!, it.longitude!!) ) }
    }

    // TODO: Make icon reactive to LiveData in VM
    @Suppress("DEPRECATION")
    private fun setFabGpsIcon(gpsIcon: MapViewModel.GpsFabState) {
        when (gpsIcon) {
            GPS_OFF -> gpsFab.setImageDrawable(resources.getDrawable(R.drawable.gps_off))
            GPS_NO_FIX -> gpsFab.setImageDrawable(resources.getDrawable(R.drawable.gps_no_fix))
            GPS_FIX -> gpsFab.setImageDrawable(resources.getDrawable(R.drawable.gps_fix))
        }
    }
}
