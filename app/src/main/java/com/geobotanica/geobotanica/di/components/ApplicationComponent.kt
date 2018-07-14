package com.geobotanica.geobotanica.di.components

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.di.modules.ApplicationModule
import com.geobotanica.geobotanica.di.modules.RepoModule
import com.geobotanica.geobotanica.di.modules.ViewModelModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class, RepoModule::class, ViewModelModule::class])
interface ApplicationComponent {
    fun inject(application: Application)

    //Expose to dependants of this component
    fun context(): Context
    fun sharedPrefs(): SharedPreferences
    fun locationService(): LocationService

    fun userRepo(): UserRepo
    fun plantRepo(): PlantRepo
    fun plantLocationRepo(): PlantLocationRepo
    fun photoRepo(): PhotoRepo
    fun measurementRepo(): MeasurementRepo

//    fun plantDetailViewModelFactory(): PlantDetailViewModelFactory // Not required?
}
