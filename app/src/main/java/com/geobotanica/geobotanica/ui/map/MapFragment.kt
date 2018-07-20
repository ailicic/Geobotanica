package com.geobotanica.geobotanica.ui.map

import android.Manifest
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.PlantComposite
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getDouble
import com.geobotanica.geobotanica.util.putDouble
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_map.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import javax.inject.Inject

// TODO: Add GPS button to bottom left of map to toggle GPS

// TODO: Show snackbar after plant saved (pass as param in Navigate)

// TODO: Decide on Lg.v/d/i etc.

// TODO: Create download map activity and utilize offline map tiles
// https://github.com/osmdroid/osmdroid/wiki/Offline-Map-Tiles

// TODO: Group nearby markers into clusters

// TODO: Double check proper placement of methods in lifecycle callbacks


/**
 * A placeholder fragment containing a simple view.
 */
class MapFragment : BaseFragment() {
    @Inject lateinit var mapViewModelFactory: MapViewModelFactory
    private lateinit var viewModel: MapViewModel

    override val className = this.javaClass.name.substringAfterLast('.')

    private var locationMarker: Marker? = null

    // Map Defaults
    private val defaultMapZoomLevel = 16.0
    private val defaultMapLatitude = 49.477
    private val defaultMapLongitude = -119.59

    // Permissions
    private val requestFineLocationPermission = 1
    private val requestExternalStoragePermission = 2

    // SharedPrefs
    private val mapSharedPres = "MapSharedPrefs"
    private val alreadyNotifiedGpsRequired = "alreadyNotifiedGpsRequired"
    private val sharedPrefsMapLatitude = "sharedPrefsMapLatitude"
    private val sharedPrefsMapLongitude = "sharedPrefsMapLongitude"
    private val sharedPrefsMapZoomLevel = "sharedPrefsMapZoomLevel"


    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = ViewModelProviders.of(this, mapViewModelFactory).get(MapViewModel::class.java)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkGpsEnabled()
        requestPermissions()
        initializeMap()
        addPlantMarkers()
        setClickListeners()
        observeLocation()
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
        viewModel.stopGps()

        saveStateToViewModel()
    }

    override fun onDetach() {
        super.onDetach()
        saveSharedPrefs()
    }
    private fun loadSharedPrefsToViewModel() {
        val sharedPrefs = activity.getSharedPreferences(mapSharedPres, MODE_PRIVATE)
        viewModel.mapPosition = GeoPoint(
                sharedPrefs.getDouble(sharedPrefsMapLatitude, defaultMapLatitude),
                sharedPrefs.getDouble(sharedPrefsMapLongitude, defaultMapLongitude)
        )
        viewModel.mapZoomLevel = sharedPrefs.getDouble(sharedPrefsMapZoomLevel, defaultMapZoomLevel)
        viewModel.alreadyNotifiedGpsRequired = sharedPrefs.getBoolean(alreadyNotifiedGpsRequired, false)
        viewModel.mapWasCenteredOnCurrentLocationOnce = false
    }

    private fun saveStateToViewModel() {
        viewModel.mapZoomLevel = map.zoomLevelDouble

        viewModel.mapPosition = map.mapCenter as GeoPoint
        Lg.d("Saved mapZoomLevel=${viewModel.mapZoomLevel}")
        Lg.d("Saved mapPosition=${viewModel.mapPosition}")
    }

    private fun saveSharedPrefs() {
        val sharedPrefs = appContext.getSharedPreferences(mapSharedPres, MODE_PRIVATE).edit()
        sharedPrefs.putBoolean(alreadyNotifiedGpsRequired, viewModel.alreadyNotifiedGpsRequired)
        sharedPrefs.putDouble(sharedPrefsMapLatitude, viewModel.mapPosition!!.latitude)
        sharedPrefs.putDouble(sharedPrefsMapLongitude, viewModel.mapPosition!!.longitude)
        sharedPrefs.putDouble(sharedPrefsMapZoomLevel, viewModel.mapZoomLevel!!)
        sharedPrefs.apply()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            requestFineLocationPermission -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Lg.i("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_GRANTED")
                    viewModel.startGps()
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

    private fun checkGpsEnabled() {
        if (viewModel.isGpsEnabled()) {
            Lg.d("GPS is already enabled")
        } else {
            Lg.d("GPS is disabled")
            if (!viewModel.alreadyNotifiedGpsRequired) {
                Snackbar.make(coordinator, R.string.gps_must_be_enabled, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.got_it) { }.show()
                viewModel.alreadyNotifiedGpsRequired = true
            }

        }
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Lg.d("GPS permissions already available. Starting GPS...")
            viewModel.startGps()
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

    private fun initializeMap() {
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(viewModel.mapZoomLevel!!)
        mapController.setCenter(viewModel.mapPosition)
    }

    private fun addPlantMarkers() {
        viewModel.allPlantComposites.observe(this, Observer<List<PlantComposite>> {
            map.overlays.clear()
            it?.forEach { plantComposite ->
                Lg.d("Adding plant marker: $plantComposite")
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
            map.postInvalidate()
        })
    }

    private fun setClickListeners() {
        fab.setOnClickListener { _ ->
            val bundle = bundleOf("userId" to viewModel.userId)
            val navController = activity.findNavController(R.id.fragment)
            navController.navigate(R.id.newPlantTypeFragment, bundle)
        }
    }

    private fun observeLocation() {
        locationMarker = null
        viewModel.currentLocation.observe(this, Observer<Location> {
            Lg.v("MapFragment:ObserveLocation = $it")
            if (it.latitude != null && it.longitude != null) {
                if (locationMarker == null)
                    createLocationMarker(it)
                else
                    updateLocationMarkerPosition(it)
                map.invalidate()
            }
            // Update satellitesInUse
        })
    }

    @Suppress("DEPRECATION")
    private fun createLocationMarker(currentLocation: Location) {
        locationMarker = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setIcon(resources.getDrawable(R.drawable.person))
            setOnMarkerClickListener { _, _ ->
                Toast.makeText(activity, "You are here", Toast.LENGTH_SHORT).show()
                true
            }
            map.overlays.add(this)
        }
        updateLocationMarkerPosition(currentLocation)
        if(!viewModel.mapWasCenteredOnCurrentLocationOnce) {
            centerMapOnCurrentLocation(currentLocation)
            viewModel.mapWasCenteredOnCurrentLocationOnce = true
        }
    }

    private fun updateLocationMarkerPosition(currentLocation: Location) {
        currentLocation.let { locationMarker?.position?.setCoords(it.latitude!!, it.longitude!!) }
    }

    private fun centerMapOnCurrentLocation(currentLocation: Location) {
        currentLocation.let { map.controller.setCenter( GeoPoint(it.latitude!!, it.longitude!!) ) }
    }
}
