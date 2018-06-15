package com.geobotanica.geobotanica.di.components

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.di.modules.ApplicationModule
import com.geobotanica.geobotanica.ui.map.MapFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {
    fun inject(application: Application)

    //Expose to dependants of this component
    fun context(): Context
    fun sharedPrefs(): SharedPreferences
    fun gbDatabase(): GbDatabase
    fun locationManager(): LocationManager
}
