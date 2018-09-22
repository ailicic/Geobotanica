package com.geobotanica.geobotanica.ui.map

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.PlantComposite
import com.geobotanica.geobotanica.data.entity.User
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SharedPrefsExt.get
import com.geobotanica.geobotanica.util.SharedPrefsExt.putSharedPrefs
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
    private var staleGpsTimer: CountDownTimer? = null

    // SharedPrefs
    private val sharedPrefsIsFirstRun = "isFirstRun"
    private val spWasNotifiedGpsRequired = "wasNotifiedGpsRequired"
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
        if (!wasPermissionGranted(ACCESS_FINE_LOCATION))
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

    // WARNING: CHECK WHICH IMPORTS ARE APPROPRIATE FOR THE VIEW MODEL (most ppl don't allow android or view imports, even if no state is stored)
    // Need to make the view as dumb as possible without creating a view dependency within the viewModel

    // TODO: Move to VM -> viewModel.saveToSharedPrefs(sharedPrefs) -> NOPE: Android import
    private fun saveSharedPrefsFromViewModel() {
        putSharedPrefs(
            sharedPrefsIsFirstRun to false,
            spWasNotifiedGpsRequired to viewModel.wasNotifiedGpsRequired,
            sharedPrefsMapLatitude to viewModel.mapLatitude,
            sharedPrefsMapLongitude to viewModel.mapLongitude,
            sharedPrefsMapZoomLevel to viewModel.mapZoomLevel,
            sharedPrefsWasGpsSubscribed to viewModel.wasGpsSubscribed
        )
    }

    // TODO: MOVE TO VM -> viewModel.loadSharedPrefs(sharedPrefs) -> NOPE: Android import
    private fun loadSharedPrefsToViewModel() {
        sharedPrefs.let { sp ->
            viewModel.let {vm ->
                vm.isFirstRun = sp.get(sharedPrefsIsFirstRun, vm.isFirstRun)
                vm.mapLatitude = sp.get(sharedPrefsMapLatitude, vm.mapLatitude)
                vm.mapLongitude = sp.get(sharedPrefsMapLongitude, vm.mapLongitude)
                vm.mapZoomLevel = sp.get(sharedPrefsMapZoomLevel, vm.mapZoomLevel)
                vm.wasNotifiedGpsRequired = sp.get(spWasNotifiedGpsRequired, vm.wasNotifiedGpsRequired)
                vm.wasGpsSubscribed = sp.get(sharedPrefsWasGpsSubscribed, vm.wasGpsSubscribed)
            }
        }
    }

    // TODO: MOVE TO VM -> viewModel.saveMapState(map) -> NOPE: Android import
    private fun saveMapStateToViewModel() {
        viewModel.mapZoomLevel = map.zoomLevelDouble
        viewModel.mapLatitude = map.mapCenter.latitude
        viewModel.mapLongitude = map.mapCenter.longitude
        viewModel.wasGpsSubscribed = viewModel.isGpsSubscribed()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            getRequestCode(ACCESS_FINE_LOCATION) -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Lg.i("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_GRANTED")
                    init()
                } else {
                    Lg.i("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_DENIED")
                }
            }
            getRequestCode(WRITE_EXTERNAL_STORAGE) -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Lg.i("onRequestPermissionsResult(): permission.WRITE_EXTERNAL_STORAGE: PERMISSION_GRANTED")
                    navigateToNewPlantType() // TODO: Confirm that this flow is correct in all cases (c.f. download?)
                } else {
                    Lg.i("onRequestPermissionsResult(): permission.WRITE_EXTERNAL_STORAGE: PERMISSION_DENIED")
                }
            }
            else -> { }
        }
    }

    private fun init() {
        initMap()
        initMapMarkers()
        observeLocation()
        initGpsSuscribe()
        setClickListeners()
    }

    private fun initMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(viewModel.mapZoomLevel)
        mapController.setCenter( GeoPoint(viewModel.mapLatitude, viewModel.mapLongitude) )
        coordinatorLayout.visibility = View.VISIBLE // TODO: Remove this after LoginScreen implemented

//        if (viewModel.isFirstRun)
//            viewModel.getLastLocation()?.let { centerMapOnLocation(it, false) } // TODO: CHECK IF THIS SHOULD STAY (never worked)
    }

    /** STRATEGY
     * Expose plantMarkerData object as LiveData which contains non-android Data
     * - PlantId
     * - Common Name
     * - Latin Name
     * - Date created
     * - Icon type -> Requires import for R (ok, not android)
     */


    // NOTE: Considered moving logic into viewModel. Very little logic can be moved.
    // For added complexity of exposing a PlantMakerData LiveData object from viewModel, it isn't worth it

    // Might consider this approach:
    // Wrap the plantMarkerData into a object describing diffs: ADD/REMOVE/UPDATE. Then forEach is removed
    // and map does not need to be cleared (better for scalability), only invalidated
    private fun initMapMarkers() {
        locationMarker = null
        locationPrecisionCircle = null

        viewModel.allPlantComposites.removeObservers((this)) // Avoids multiple subscriptions to LiveData!
        viewModel.allPlantComposites.observe(this, Observer<List<PlantComposite>> {
            Lg.d("addPlantMarkers(): Adding ${it.size} plants")
            map.overlays.clear()

            it?.forEach { plantComposite ->
                Lg.v("Adding plant marker: $plantComposite")
                val plantId = plantComposite.plant.id
                val plantMarker = GbMarker(activity, plantId, map)
                val plantLocation = plantComposite.plantLocations.first().location
                val plantPhoto = plantComposite.photos.first()

                // TODO: Consider using a custom InfoWindow
                // https://code.google.com/archive/p/osmbonuspack/wikis/Tutorial_2.wiki
                // 7. Customizing the bubble behaviour:
                // 9. Creating your own bubble layout

                var icon = 0
                with(plantMarker) {
                    plantComposite.plant.let {
                        title = it.commonName
                        snippet = it.latinName
                        subDescription = it.timestamp.toString().substringBefore('T')

                        icon = when (it.type) {
                            Plant.Type.TREE -> R.drawable.marker_purple
                            Plant.Type.SHRUB -> R.drawable.marker_blue
                            Plant.Type.HERB -> R.drawable.marker_green
                            Plant.Type.GRASS -> R.drawable.marker_light_green
                            Plant.Type.VINE -> R.drawable.marker_yellow
                        }
                    }

                    @Suppress("DEPRECATION") setIcon(resources.getDrawable(icon))
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    setOnMarkerClickListener { marker: Marker, _ ->
                        if (marker.isInfoWindowOpen)
                            marker.closeInfoWindow() // TODO: Destroy drawable here
                        else
                            marker.showInfoWindow() // TODO: Create drawable here
                        true
                    }

                    position.setCoords(plantLocation.latitude!!, plantLocation.longitude!!)
                    image = Drawable.createFromPath(plantPhoto.fileName)
                    map.overlays.add(this)
                }
            }
            locationMarker?.let { map.overlays.add(it) }
            map.postInvalidate()
        })
    }


    // TODO: MOVE SOME TO VM (use livedata to trigger snackbar, fab icon change or navigation
        // Use SingleLiveEvent for navigation, snackbar, etc.
        // https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150
    // NOTE: Must keep permission check here as depends on activity +others
        // -> OR... Create boolean LiveData to
    @Suppress("DEPRECATION")
    private fun setClickListeners() {
        fabGps.setOnClickListener { _ ->

            // MOVE -> else { viewModel.onGpsFabClicked() }
//            else if (!viewModel.isGpsEnabled()) {
            if (!viewModel.isGpsEnabled()) {
                showGpsRequiredSnackbar(false)
                setFabGpsIcon(FabGpsIcon.GPS_OFF)
            }
            else if (viewModel.isGpsSubscribed())
                unsubscribeGps()
            else
                subscribeGps()
        }
        fabNew.setOnClickListener { _ ->    // TODO: Handle case where user permanently denies storage permission!
            if (!wasPermissionGranted(WRITE_EXTERNAL_STORAGE))
                requestPermission(WRITE_EXTERNAL_STORAGE)
            else
                navigateToNewPlantType()
        }
    }

    private fun navigateToNewPlantType() {
        val bundle = bundleOf("userId" to viewModel.userId)
        val navController = activity.findNavController(R.id.fragment)
        navController.navigate(R.id.newPlantTypeFragment, bundle)
    }

    // OPPORTUNITIES TO MOVE LOGIC TO VM
    /**
     * Create LiveData for
     * - UpdateLocationMarkerPosition boolean
     * - SetGpsFabIcon enum
     * - UpdateLocationPrecision boolean
     * - CenterMapOnLocation boolean
     * - (Timer will already be in VM)
     * - Invalidate map boolean
     *
     */
    @Suppress("DEPRECATION")
    private fun observeLocation() {
        viewModel.currentLocation.observe(this, Observer<Location> {
            Lg.v("MapFragment:ObserveLocation = $it")
            if (it.latitude != null && it.longitude != null) {
                updateLocationMarkerPosition(it)

                if (it.isRecent()) {
                    setFabGpsIcon(FabGpsIcon.GPS_FIX)
                    updateLocationPrecision(it)
                    if (isLocationOffScreen(it))
                        centerMapOnLocation(it)
                    setStaleGpsLocationTimer()
                }
                map.invalidate()
            }
            // TODO: Update satellitesInUse
        })
    }

    // TODO: Move to VM (all should be fine if FabIcon has LiveData)
    private fun setStaleGpsLocationTimer() {
        staleGpsTimer?.run { cancel() }
        staleGpsTimer = object: CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) { }

            override fun onFinish() {
                Lg.d("staleGpsTimer finished.")
                setFabGpsIcon(FabGpsIcon.GPS_NO_FIX)
            }
        }.start()
    }

    // TODO: Move into UpdateLocationPrecision LiveData observe lambda
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
            map.overlays.add(this)
        }
        map.overlays.remove(locationMarker)
        map.overlays.add(locationMarker)
    }


    // TODO: MOVE TO VM -> what about wasGpsPermissionGranted()? -> OK!
    private fun initGpsSuscribe() {
        val isGpsEnabled = viewModel.isGpsEnabled()
        if (!viewModel.wasNotifiedGpsRequired && !isGpsEnabled) {
            showGpsRequiredSnackbar(true)
            viewModel.wasNotifiedGpsRequired = true
        }

        // TODO: Remove this permission check after permissions redesigned
        if (viewModel.wasGpsSubscribed && isGpsEnabled )
            subscribeGps()
    }

    // TODO: Make snackbars reactive to LiveData in VM
    private fun showGpsRequiredSnackbar(isPersistent: Boolean) {
        if (isPersistent) {
            Snackbar.make(coordinatorLayout, R.string.gps_must_be_enabled, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.got_it) { }
                .show()
        } else {
            Snackbar.make(coordinatorLayout, R.string.gps_must_be_enabled, Snackbar.LENGTH_LONG)
                .setAction(R.string.enable) {
                    startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }.show()
        }
    }

    // TODO: Make icon reactive to LiveData in VM
    @Suppress("DEPRECATION")
    private fun setFabGpsIcon(gpsIcon: FabGpsIcon) {
        when (gpsIcon) {
            FabGpsIcon.GPS_OFF -> fabGps.setImageDrawable(resources.getDrawable(R.drawable.gps_off))
            FabGpsIcon.GPS_NO_FIX -> fabGps.setImageDrawable(resources.getDrawable(R.drawable.gps_no_fix))
            FabGpsIcon.GPS_FIX -> fabGps.setImageDrawable(resources.getDrawable(R.drawable.gps_fix))
        }

    }

    // TODO: Attempt to move to VM (should be doable, use icon LiveData)
    private fun subscribeGps() { @Suppress("DEPRECATION")
        setFabGpsIcon(FabGpsIcon.GPS_NO_FIX)
        viewModel.subscribeGps()
    }

    // TODO: Attempt to move to VM (should be doable, use icon LiveData)
    private fun unsubscribeGps() { @Suppress("DEPRECATION")
        setFabGpsIcon(FabGpsIcon.GPS_OFF)
        viewModel.unsubscribeGps()
    }

    @Suppress("DEPRECATION")
    private fun createLocationMarker() {
        locationMarker = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setIcon(resources.getDrawable(R.drawable.person))
            setOnMarkerClickListener { _, _ ->
                Toast.makeText(activity, "You are here", Toast.LENGTH_SHORT).show()
                true
            }
            map.overlays.add(this)
        }
    }



    // TODO: Make reactive to LiveData in VM
    private fun updateLocationMarkerPosition(currentLocation: Location) {
        if (locationMarker == null)
            createLocationMarker()
        currentLocation.let {
            locationMarker?.position?.setCoords(it.latitude!!, it.longitude!!)
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

    enum class FabGpsIcon {
        GPS_OFF, GPS_NO_FIX, GPS_FIX
    }
}
