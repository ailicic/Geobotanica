package com.geobotanica.geobotanica.ui.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.PhotoRepo
import com.geobotanica.geobotanica.data.repo.PlantLocationRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.data.repo.UserRepo
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.newplanttype.NewPlantTypeActivity
import com.geobotanica.geobotanica.util.Lg
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_map.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import javax.inject.Inject


// TODO: Create download map activity and utilize offline map tiles
// https://github.com/osmdroid/osmdroid/wiki/Offline-Map-Tiles

// TODO: Group nearby markers into clusters

/**
 * A placeholder fragment containing a simple view.
 */
class MapFragment : BaseFragment() {
    @Inject lateinit var userRepo: UserRepo
    @Inject lateinit var plantRepo: PlantRepo
    @Inject lateinit var plantLocationRepo: PlantLocationRepo
    @Inject lateinit var photoRepo: PhotoRepo
    @Inject lateinit var locationService: LocationService




    override val name = this.javaClass.name.substringAfterLast('.')
    private var userId: Long = 0
    private val requestFineLocationPermission = 1
    private val requestExternalStorage = 2
    private var currentLocation: Location? = null
    private var locationMarker: Marker? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        (getActivity() as BaseActivity).activityComponent.inject(this)

        getGuestUserId()

        //load/initialize the osmdroid configuration, this can be done
        Configuration.getInstance().load(context, sharedPrefs)
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string
    }

    private fun getGuestUserId() {
        userRepo.get(1).observe(this, Observer<User> {
            userId = if (it != null) {
                it.id
            } else {
                userRepo.insert(User("Guest"))
            }
        })
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

    override fun onResume() {
        super.onResume()

        // TODO: Fix bug due to multiple setOnClickListerner() calls on Markers (crash when long press marker after reload)
        plantRepo.getAllPlantComposites().observe(this, Observer<List<PlantComposite>> {
            map.overlays.clear()
            it?.forEach {
                Lg.d("Adding plant marker: (id=${it.plant.id}) $it")
                val plantMarker = GbMarker(activity, it.plant.id, map)
                val plantLocation = plantLocationRepo.getPlantLocation(it.locations.first().id) // TODO: Take newest location

                // TODO: Consider using a custom InfoWindow
                // https://code.google.com/archive/p/osmbonuspack/wikis/Tutorial_2.wiki
                // 7. Customizing the bubble behaviour:
                // 9. Creating your own bubble layout

                var icon = 0
                plantMarker.run {
                    it.plant.let {
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
                    @Suppress("DEPRECATION") setIcon(activity.resources.getDrawable(icon))

                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    setOnMarkerClickListener { marker: Marker, _ ->
                        if (marker.isInfoWindowOpen)
                            marker.closeInfoWindow()
                        else
                            marker.showInfoWindow()
                        true
                    }

                    plantLocation.observe(this@MapFragment, Observer<PlantLocation> { plantLocation ->
                        position.setCoords(plantLocation?.location?.latitude!!, plantLocation.location.longitude!!)
                    })

                    photoRepo.getMainPhotoOfPlant(plantId).observe(this@MapFragment, Observer<Photo> { photo ->
                        image = Drawable.createFromPath(photo?.fileName)
                    })

                    map.overlays.add(this)
                }
            }
            locationMarker?.let { map.overlays.add(locationMarker) }
            map.postInvalidate()
        })

        fab.setOnClickListener { _ ->
            val intent = Intent(activity, NewPlantTypeActivity::class.java)
                    .putExtra(getString(R.string.extra_user_id), userId)
            startActivity(intent)
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
                    locationMarker?.run {
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

    class GbMarker(val activity: BaseActivity, val plantId: Long, map: MapView): Marker(map) {

        override fun onLongPress(event: MotionEvent?, mapView: MapView?): Boolean {
            Lg.d("Opening plant detail: id=$plantId")
            val touched = hitTest(event, mapView)
            if (touched) {
                var bundle = bundleOf("plantId" to plantId)
                val navController = activity.findNavController(R.id.fragment)
                navController.navigate(R.id.plantDetailFragment, bundle)
            }
            return touched
        }
    }
}
