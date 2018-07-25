package com.geobotanica.geobotanica.ui.map

import android.Manifest
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.PlantComposite
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SharedPrefsExt.get
import com.geobotanica.geobotanica.util.SharedPrefsExt.put
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


// LONG TERM
// TODO: Use vector graphics for all icons where possible
// TODO: Decide on Lg.v/d/i etc.
// TODO: Double check proper placement of methods in lifecycle callbacks
// TODO: Group nearby markers into clusters
// TODO: Create download map activity and utilize offline map tiles
// https://github.com/osmdroid/osmdroid/wiki/Offline-Map-Tiles




class MapFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<MapViewModel>
    private lateinit var viewModel: MapViewModel

    override val className = this.javaClass.name.substringAfterLast('.')

    private var locationMarker: Marker? = null
    private var locationPrecisionCircle: Polygon? = null
    private var staleGpsTimer: CountDownTimer? = null

    // Permissions
    private val requestFineLocationPermission = 1
    private val requestExternalStoragePermission = 2

    // SharedPrefs
    private val mapSharedPrefs = "MapSharedPrefs"
    private val sharedPrefsIsFirstRun = "isFirstRun"
    private val spWasNotifiedGpsRequired = "spWasNotifiedGpsRequired"
    private val sharedPrefsMapLatitude = "MapLatitude"
    private val sharedPrefsMapLongitude = "MapLongitude"
    private val sharedPrefsMapZoomLevel = "MapZoomLevel"
    private val sharedPrefsWasGpsSubscribed = "gpsUpdatesSubscribed"


    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = 1L // TODO: Retrieve userId from LoginFragment Navigation bundle
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onStart() {
        super.onStart()
        requestPermissions()
        initMap()
        initMapMarkers()
        observeLocation()
        initGpsSuscribe()
        setClickListeners()
    }

    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause()  //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onStop() {
        super.onStop()
        staleGpsTimer?.run { cancel(); staleGpsTimer = null }
        saveStateToViewModel()
        viewModel.unsubscribeGps()
    }

    override fun onDetach() {
        super.onDetach()
        saveSharedPrefsFromViewModel()
    }

    private fun saveSharedPrefsFromViewModel() {
        appContext.getSharedPreferences(mapSharedPrefs, MODE_PRIVATE).put(
            mapOf(
                sharedPrefsIsFirstRun to false,
                spWasNotifiedGpsRequired to viewModel.wasNotifiedGpsRequired,
                sharedPrefsMapLatitude to viewModel.mapLatitude,
                sharedPrefsMapLongitude to viewModel.mapLongitude,
                sharedPrefsMapZoomLevel to viewModel.mapZoomLevel,
                sharedPrefsWasGpsSubscribed to viewModel.wasGpsSubscribed
            )
        )
    }

    private fun loadSharedPrefsToViewModel() {
        activity.getSharedPreferences(mapSharedPrefs, MODE_PRIVATE).let {
            viewModel.let {vm ->
                vm.isFirstRun = it.get(sharedPrefsIsFirstRun, vm.isFirstRun)
                vm.mapLatitude = it.get(sharedPrefsMapLatitude, vm.mapLatitude)
                vm.mapLongitude = it.get(sharedPrefsMapLongitude, vm.mapLongitude)
                vm.mapZoomLevel = it.get(sharedPrefsMapZoomLevel, vm.mapZoomLevel)
                vm.wasNotifiedGpsRequired = it.get(spWasNotifiedGpsRequired, vm.wasNotifiedGpsRequired)
                vm.wasGpsSubscribed = it.get(sharedPrefsWasGpsSubscribed, vm.wasGpsSubscribed)
            }
        }
    }

    private fun saveStateToViewModel() {
        viewModel.mapZoomLevel = map.zoomLevelDouble
        viewModel.mapLatitude = map.mapCenter.latitude
        viewModel.mapLongitude = map.mapCenter.longitude
        viewModel.wasGpsSubscribed = viewModel.isGpsSubscribed()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            requestFineLocationPermission -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Lg.i("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_GRANTED")
                    if (viewModel.wasGpsSubscribed && viewModel.isGpsEnabled())
                        subscribeGps()
                    if (viewModel.isFirstRun)
                        viewModel.getLastLocation()?.also { centerMapOnLocation(it, false) }
                } else {
                    Lg.i("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_DENIED")
                }
            }
            requestExternalStoragePermission -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Lg.i("onRequestPermissionsResult(): permission.WRITE_EXTERNAL_STORAGE: PERMISSION_GRANTED")
                } else {
                    Lg.i("onRequestPermissionsResult(): permission.WRITE_EXTERNAL_STORAGE: PERMISSION_DENIED")
                }
            }
            else -> { }
        }
    }

    private fun requestPermissions() {
        if (wasGpsPermissionGranted()) {
            Lg.d("GPS permissions already available.")
        } else {
            Lg.d("GPS permissions not available. Requesting now...")
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestFineLocationPermission)
        }

        if (ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Lg.d("External storage permissions already available.")
        } else {
            Lg.d("External storage permissions not available. Requesting now...")
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), requestExternalStoragePermission)
        }
    }

    private fun wasGpsPermissionGranted(): Boolean = ContextCompat.checkSelfPermission(activity,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun initMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(viewModel.mapZoomLevel)
        mapController.setCenter( GeoPoint(viewModel.mapLatitude, viewModel.mapLongitude) )
    }

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

    @Suppress("DEPRECATION")
    private fun setClickListeners() {
        fabGps.setOnClickListener { _ ->
            if (!wasGpsPermissionGranted())
                requestPermissions()
            else if (!viewModel.isGpsEnabled()) {
                showGpsRequiredSnackbar(false)
                setFabGpsIcon(FabGpsIcon.GPS_OFF)
            }
            else if (viewModel.isGpsSubscribed())
                unsubscribeGps()
            else
                subscribeGps()
        }
        fabNew.setOnClickListener { _ ->
            if (!wasGpsPermissionGranted())
                requestPermissions()
            else if (!viewModel.isGpsEnabled())
                showGpsRequiredSnackbar(false)
            else {
                val bundle = bundleOf("userId" to viewModel.userId)
                val navController = activity.findNavController(R.id.fragment)
                navController.navigate(R.id.newPlantTypeFragment, bundle)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun observeLocation() {
        viewModel.currentLocation.observe(this, Observer<Location> {
            Lg.v("MapFragment:ObserveLocation = $it")
            if (it.latitude != null && it.longitude != null) {
                updateLocationMarkerPosition(it)

                if (it.notCached()) {
                    setFabGpsIcon(FabGpsIcon.GPS_FIX)
                    updateLocationPrecision(it)
                    if (locationOffScreen(it))
                        centerMapOnLocation(it)
                    setStaleGpsLocationTimer()
                }
                map.invalidate()
            }
            // TODO: Update satellitesInUse
        })
    }

    private fun setStaleGpsLocationTimer() {
        staleGpsTimer?.run { cancel(); staleGpsTimer = null }
        staleGpsTimer = object: CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) { }

            override fun onFinish() {
                Lg.d("staleGpsTimer finished.")
                setFabGpsIcon(FabGpsIcon.GPS_NO_FIX)
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
            map.overlays.add(this)
        }
        map.overlays.remove(locationMarker)
        map.overlays.add(locationMarker)
    }

    private fun initGpsSuscribe() {
        val isGpsEnabled = viewModel.isGpsEnabled()
        if (!viewModel.wasNotifiedGpsRequired && !isGpsEnabled) {
            showGpsRequiredSnackbar(true)
            viewModel.wasNotifiedGpsRequired = true
        }
        if (viewModel.wasGpsSubscribed && isGpsEnabled && wasGpsPermissionGranted())
            subscribeGps()
    }

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

    @Suppress("DEPRECATION")
    private fun setFabGpsIcon(gpsIcon: FabGpsIcon) {
        when (gpsIcon) {
            FabGpsIcon.GPS_OFF -> fabGps.setImageDrawable(resources.getDrawable(R.drawable.gps_off))
            FabGpsIcon.GPS_NO_FIX -> fabGps.setImageDrawable(resources.getDrawable(R.drawable.gps_no_fix))
            FabGpsIcon.GPS_FIX -> fabGps.setImageDrawable(resources.getDrawable(R.drawable.gps_fix))
        }

    }

    private fun subscribeGps() { @Suppress("DEPRECATION")
        setFabGpsIcon(FabGpsIcon.GPS_NO_FIX)
        viewModel.subscribeGps()
    }

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

    private fun updateLocationMarkerPosition(currentLocation: Location) {
        if (locationMarker == null)
            createLocationMarker()
        currentLocation.let {
            locationMarker?.position?.setCoords(it.latitude!!, it.longitude!!)
        }
    }

    private fun locationOffScreen(location: Location): Boolean =
        !map.projection.boundingBox.contains(location.latitude!!, location.longitude!!)

    private fun centerMapOnLocation(location: Location, animate: Boolean = true) {
        animate?.run {
            location.let { map.controller.animateTo( GeoPoint(it.latitude!!, it.longitude!!) ) }
        } ?: location.let { map.controller.setCenter( GeoPoint(it.latitude!!, it.longitude!!) ) }
    }

    enum class FabGpsIcon {
        GPS_OFF, GPS_NO_FIX, GPS_FIX
    }
}
