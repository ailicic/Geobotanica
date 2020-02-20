package com.geobotanica.geobotanica.ui

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.location.Location
import com.geobotanica.geobotanica.di.components.ApplicationComponent
import com.geobotanica.geobotanica.di.components.DaggerApplicationComponent
import com.geobotanica.geobotanica.di.modules.ApplicationModule
import com.geobotanica.geobotanica.di.modules.RepoModule
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.isEmulator
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.android.synthetic.main.activity_main.*
import org.mapsforge.map.android.graphics.AndroidGraphicFactory

// TODO: Consider extracting all notification channel code to NotificationChannelHelper
const val NOTIFICATION_CHANNEL_ID_DOWNLOADS = "com.geobotanica.geobotanica.downloads"

class MainActivity : AppCompatActivity() {
    lateinit var applicationComponent: ApplicationComponent

    private val className = "MainActivity"
    private var notificationManager: NotificationManager? = null

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


        registerReceiver(onClickDownloadNotification, IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager =
                    ContextCompat.getSystemService<NotificationManager>(this, NotificationManager::class.java)
            createNotificationChannels()
        }

        AndroidThreeTen.init(this)

        if (isEmulator())
            Lg.d("Running on emulator")
        else
            Lg.d("Running on device")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannels() {
        createNotificationChannel(
                R.string.notification_channel_downloads,
                R.string.notification_channel_description_downloads,
                NOTIFICATION_CHANNEL_ID_DOWNLOADS
        )
    }

    @Suppress("SameParameterValue")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(nameResId: Int, descriptionResId: Int, id: String) {
        val name = applicationContext.getString(nameResId)
        if (! channelExists(id)) {
            Lg.d("MainActivity: Creating notification channel: $name")
            val channel = NotificationChannel(id, name, IMPORTANCE_LOW)
            channel.description = applicationContext.getString(descriptionResId)
            notificationManager?.createNotificationChannel(channel)
        } else
            Lg.d("MainActivity: Skipped creating existing channel: $name")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun channelExists(channelId: String): Boolean {
        val channel = notificationManager?.notificationChannels?.firstOrNull { it.id == channelId }
        return channel != null
    }

    override fun onSupportNavigateUp() = findNavController(R.id.fragment).navigateUp()

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
        unregisterReceiver(onClickDownloadNotification)
    }
}