package com.geobotanica.geobotanica.di.components

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.di.modules.ApplicationModule
import com.geobotanica.geobotanica.di.modules.RepoModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class, RepoModule::class])
interface ApplicationComponent {
    fun inject(application: Application)

    //Expose to dependants of this component
    fun context(): Context
    fun sharedPrefs(): SharedPreferences
    fun locationManager(): LocationManager

    fun userRepo(): UserRepo
    fun plantRepo(): PlantRepo
    fun locationRepo(): LocationRepo
    fun photoRepo(): PhotoRepo
    fun measurementRepo(): MeasurementRepo
}
