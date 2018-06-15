package com.geobotanica.geobotanica.di.modules

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import android.preference.PreferenceManager
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.di.PerActivity
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ApplicationModule(private val context: Context) {
    @Provides @Singleton fun provideApplicationContext(): Context = context

    @Provides @Singleton fun provideSharedPrefs(): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)

    @Provides @Singleton fun provideLocationManager(): LocationManager =
         context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

//     Not required due to constructor injection:
//    @Provides @Singleton fun provideLocationService(locationManager: LocationManager) = LocationService(locationManager)
}
