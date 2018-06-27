package com.geobotanica.geobotanica.ui.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.repo.LocationRepo
import com.geobotanica.geobotanica.data.repo.PhotoRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.data.repo.UserRepo
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.plantdetail.PlantDetailActivity
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.fragment_map.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import javax.inject.Inject


// TODO: Create download map activity and utilize offline map tiles
// https://github.com/osmdroid/osmdroid/wiki/Offline-Map-Tiles

/**
 * A placeholder fragment containing a simple view.
 */
class MapFragment : BaseFragment() {
    @Inject lateinit var userRepo: UserRepo
    @Inject lateinit var plantRepo: PlantRepo
    @Inject lateinit var locationRepo: LocationRepo
    @Inject lateinit var photoRepo: PhotoRepo
    @Inject lateinit var locationService: LocationService

    override val name = this.javaClass.name.substringAfterLast('.')
    private val requestFineLocationPermission = 1
    private val requestExternalStorage = 2
    private var currentLocation: Location? = null
    private var locationMarker: Marker? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        (getActivity() as BaseActivity).activityComponent.inject(this)

        //load/initialize the osmdroid configuration, this can be done
        Configuration.getInstance().load(context, sharedPrefs)
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

        // TODO: Try to push this code into LocationService.
        if (ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Lg.d("GPS permissions already available. Subscribing now...")
        } else {
            Lg.d("Requesting GPS permissions now...")
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestFineLocationPermission)
        }

        if(ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            Lg.d("MapFragment: External storage permissions already available.")
        } else {
            Lg.d("MapFragment: External storage permissions not available. Requesting now...")
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), requestExternalStorage)
        }

        if (!locationService.isGpsEnabled()) {
            Lg.d("GPS disabled")
            Snackbar.make(activity.findViewById(android.R.id.content), "Please enable GPS", Snackbar.LENGTH_LONG).setAction("Action", null).show()
        }
        else
            Lg.d("GPS enabled")

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        @Suppress("DEPRECATION") mapController.setZoom(16)
        val startPoint = GeoPoint(49.477, -119.59)
        mapController.setCenter(startPoint)
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationService.subscribe(activity, ::onLocation)
        }
    }

    class GbMarker(val plantId: Long, map: MapView): Marker(map) {
        var longPressCallback: () -> Unit = { }

        override fun onLongPress(event: MotionEvent?, mapView: MapView?): Boolean {
            longPressCallback()
            return super.onLongPress(event, mapView)
        }
    }

    override fun onResume() {
        super.onResume()

        plantRepo.getAll().forEach {
            Lg.d("Adding plant marker: (id=${it.id}) $it")
            val location = locationRepo.getPlantLocation(it.id)
            val plantMarker = GbMarker(it.id, map)

            // TODO: Consider using a custom InfoWindow
            // https://code.google.com/archive/p/osmbonuspack/wikis/Tutorial_2.wiki
            // 7. Customizing the bubble behaviour:
            // 9. Creating your own bubble layout
            plantMarker.apply {
                title = it.commonName
                snippet = it.latinName
                subDescription = it.timestamp.toString().substringBefore('T')
                image = Drawable.createFromPath(photoRepo.getAllPhotosOfPlant(it.id)[0].fileName)
                val icon = when (it.type) {
                    Plant.Type.TREE.ordinal -> R.drawable.marker_purple
                    Plant.Type.SHRUB.ordinal -> R.drawable.marker_blue
                    Plant.Type.HERB.ordinal -> R.drawable.marker_green
                    Plant.Type.VINE.ordinal -> R.drawable.marker_yellow
                    else -> R.drawable.marker_yellow
                }
                @Suppress("DEPRECATION") setIcon(activity.resources.getDrawable(icon))
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { marker: Marker, _ ->
                    if (marker.isInfoWindowOpen)
                        marker.closeInfoWindow()
                    else
                        marker.showInfoWindow()
                    true
                }
                longPressCallback = {
                    val intent = Intent(activity, PlantDetailActivity::class.java)
                            .putExtra(getString(R.string.extra_user_id), (activity as MapActivity).userId)
                            .putExtra(getString(R.string.extra_plant_id), plantId)
                    startActivity(intent)
                }

                position.setCoords(location.latitude!!, location.longitude!!)
                map.overlays.add(this)
            }
        }

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
        locationService.unsubscribe(activity)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            requestFineLocationPermission -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Lg.d("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_GRANTED")
                    locationService.subscribe(activity, ::onLocation)
                } else {
                    Lg.d("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_DENIED")
                }
            }
            requestExternalStorage -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Lg.d("onRequestPermissionsResult(): permission.WRITE_EXTERNAL_STORAGE: PERMISSION_GRANTED")
                } else {
                    Lg.d("onRequestPermissionsResult(): permission.WRITE_EXTERNAL_STORAGE: PERMISSION_DENIED")
                }
            }
            else -> { } // Ignore all other requests.
        }
    }

    @Suppress("DEPRECATION")
    private fun onLocation(location: Location) {
        currentLocation = location
        with(location) {
            Lg.d("onLocation(): $this")

            if (latitude != null && longitude != null) {
                val _latitude: Double = latitude!!
                val _longitude: Double = longitude!!
                if (locationMarker == null) {
                    locationMarker = Marker(map)
                    locationMarker?.apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setIcon(activity.getResources().getDrawable(R.drawable.person))
                        setOnMarkerClickListener { _, _ ->
                            Toast.makeText(appContext, "You are here", Toast.LENGTH_SHORT).show()
                            true
                        }
                        position.setCoords(_latitude, _longitude)
                        map.overlays.add(this)
                        map.controller.setCenter(GeoPoint(_latitude, _longitude))
                    }
                } else
                    locationMarker?.position?.setCoords(latitude!!, longitude!!)
            }
        }
        map.invalidate()
    }
}
