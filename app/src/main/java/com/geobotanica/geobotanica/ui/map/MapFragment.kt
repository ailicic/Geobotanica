package com.geobotanica.geobotanica.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.dao.UserDao
import com.geobotanica.geobotanica.data.entity.User
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.util.Lg
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_map.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint

// https://github.com/osmdroid/osmdroid/wiki/Offline-Map-Tiles

/**
 * A placeholder fragment containing a simple view.
 */
class MapFragment : BaseFragment() {
//    @Inject lateinit var activity: BaseActivity
//    @Inject lateinit var sharedPrefs: SharedPreferences
//    @Inject lateinit var gbDatabase: GbDatabase
    private val requestExternalStorage = 3


    override fun onAttach(context: Context) {
        super.onAttach(context)
//        (getActivity() as BaseActivity).activityComponent.inject(this)

        //load/initialize the osmdroid configuration, this can be done
        Configuration.getInstance().load(context, sharedPrefs)
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        Observable.just(gbDatabase)
                .subscribeOn(Schedulers.io())
                .subscribe { gbDatabase ->
                    val userDao: UserDao = gbDatabase.userDao()
                    userDao.insert(User("Alan"))
                    val users: List<User> = userDao.getAll()
                    users.forEach{
                        Lg.d(it.nickName)
                    }
                }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {


        if(ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            Lg.d("MapFragment: External storage permissions already available.")
        } else {
            Lg.d("MapFragment: External storage permissions not available. Requesting now...")
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), requestExternalStorage)
        }

        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //handle permissions first, before map is created. not depicted here

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)

        // Set map start point
        val mapController = map.controller
        mapController.setZoom(14)
        val startPoint = GeoPoint(49.477, -119.59)
        mapController.setCenter(startPoint)
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

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
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
}
