package com.geobotanica.geobotanica.di.modules

import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import android.preference.PreferenceManager
import com.geobotanica.geobotanica.network.online_map.OnlineMapEntry
import com.geobotanica.geobotanica.network.online_map.OnlineMapFile
import com.geobotanica.geobotanica.network.online_map.OnlineMapFolder
import com.geobotanica.geobotanica.ui.MainActivity
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
class ApplicationModule(private val appContext: Context, private val activity: MainActivity) {
    @Provides @Singleton fun provideApplicationContext(): Context = appContext

    @Provides @Singleton fun provideActivity(): MainActivity = activity

    @Provides @Singleton fun provideSharedPrefs(): SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(appContext)

    @Provides @Singleton fun provideLocationManager(): LocationManager =
            appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Provides @Singleton fun provideOkHttpClient(): OkHttpClient = OkHttpClient()
    @Provides @Singleton fun provideMoshi(): Moshi = Moshi.Builder()
            .add(
                    PolymorphicJsonAdapterFactory.of(OnlineMapEntry::class.java, "entryType")
                            .withSubtype(OnlineMapFile::class.java, "file")
                            .withSubtype(OnlineMapFolder::class.java, "folder")
            )
            .build()

//     Not required due to constructor injection:
//    @Provides @Singleton fun provideLocationService(locationManager: LocationManager) = LocationService(locationManager)
}
