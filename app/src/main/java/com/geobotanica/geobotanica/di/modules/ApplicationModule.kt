package com.geobotanica.geobotanica.di.modules

import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import android.preference.PreferenceManager
import com.geobotanica.geobotanica.ui.MainActivity
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ApplicationModule(private val appContext: Context, private val activity: MainActivity) {
    @Provides @Singleton fun provideApplicationContext(): Context = appContext

    @Provides @Singleton fun provideActivity(): MainActivity = activity

//    @Provides @Singleton fun provideResources(): Resources = appContext.resources

    @Provides @Singleton fun provideSharedPrefs(): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(appContext)

    @Provides @Singleton fun provideLocationManager(): LocationManager =
            appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

//    @Provides @Singleton fun provideConnectivityManager(): ConnectivityManager =
//            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

//     Not required due to constructor injection:
//    @Provides @Singleton fun provideLocationService(locationManager: LocationManager) = LocationService(locationManager)
}
