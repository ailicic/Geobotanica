package com.geobotanica.geobotanica.di.modules

import android.app.Activity
import android.app.Application
import android.content.Context
import android.location.LocationManager
import com.geobotanica.geobotanica.android.location.LocationService
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ApplicationModule(private val application: Application) {
    @Provides @Singleton fun provideApplication(): Application = application
//    @Provides @Singleton fun provideActivity(): Activity = activity
    @Provides @Singleton fun provideLocationManager(): LocationManager {
        return application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    @Provides @Singleton fun provideLocationService(locationManager: LocationManager): LocationService {
        return LocationService(application, locationManager)
    }
}
//    @Provides @Singleton @ForApplication fun provideApplicationContext(application: Application): Context = application
