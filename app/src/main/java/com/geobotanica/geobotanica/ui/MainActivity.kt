package com.geobotanica.geobotanica.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.di.components.ApplicationComponent
import com.geobotanica.geobotanica.di.components.DaggerApplicationComponent
import com.geobotanica.geobotanica.di.modules.ApplicationModule
import com.geobotanica.geobotanica.di.modules.RepoModule
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.isEmulator
import kotlinx.android.synthetic.main.activity_main.*
import org.mapsforge.map.android.graphics.AndroidGraphicFactory

class MainActivity : AppCompatActivity() {
    lateinit var applicationComponent: ApplicationComponent

    private val className = "MainActivity"

    private val _downloadComplete = MutableLiveData<Long>()
    val downloadComplete: LiveData<Long> = _downloadComplete // Emits downloadId. Used by FileDownloader.

    // TODO: Either serialize this into bundle or store it some other way
    var currentLocation: Location? = null // Easy solution to hold location between fragments during new plant flow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lg.v("$className: onCreate()")

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val navController = findNavController(R.id.fragment)
        setupActionBarWithNavController(this, navController)

        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(applicationContext, this))
                .repoModule(RepoModule())
                .build()

        applicationComponent.inject(this)

        AndroidGraphicFactory.createInstance(application) // Required by MapsForge
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        registerReceiver(onClickDownloadNotification, IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED))

        if (isEmulator())
            Lg.d("Running on emulator")
        else
            Lg.d("Running on device")
    }

    override fun onSupportNavigateUp() = findNavController(R.id.fragment).navigateUp()

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            _downloadComplete.value = downloadId
        }
    }

    private val onClickDownloadNotification = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
        }
    }


    override fun onStart() {
        super.onStart()
        Lg.v("$className: onStart()")
    }

    override fun onResume() {
        super.onResume()
        Lg.v("$className: onResume()")
    }

    override fun onPause() {
        super.onPause()
        Lg.v("$className: onPause()")
    }

    override fun onStop() {
        super.onStop()
        Lg.v("$className: onStop()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Lg.v("$className: onDestroy()")
        AndroidGraphicFactory.clearResourceMemoryCache()
        unregisterReceiver(onDownloadComplete)
        unregisterReceiver(onClickDownloadNotification)
    }
}