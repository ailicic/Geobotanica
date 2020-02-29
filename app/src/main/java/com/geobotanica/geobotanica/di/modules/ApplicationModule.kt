package com.geobotanica.geobotanica.di.modules

import android.app.DownloadManager
import android.content.Context
import android.location.LocationManager
import androidx.work.WorkManager
import com.geobotanica.geobotanica.util.DefaultDispatchers
import com.geobotanica.geobotanica.util.GbDispatchers
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
class ApplicationModule(private val appContext: Context) {

    @Provides @Singleton fun provideApplicationContext(): Context = appContext

    @Provides @Singleton fun provideDispatchers(): GbDispatchers = DefaultDispatchers()

    @Provides @Singleton fun provideLocationManager(): LocationManager =
            appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Provides @Singleton fun provideDownloadManager(): DownloadManager =
            appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    @Provides @Singleton fun provideOkHttpClient(): OkHttpClient = OkHttpClient()
    @Provides @Singleton fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides @Singleton fun provideWorkManager(): WorkManager = WorkManager.getInstance(appContext)
}
