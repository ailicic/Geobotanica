package com.geobotanica.geobotanica.di.modules

import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import android.preference.PreferenceManager
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.dao.LocationDao
import com.geobotanica.geobotanica.data.dao.PhotoDao
import com.geobotanica.geobotanica.data.dao.PlantDao
import com.geobotanica.geobotanica.data.dao.UserDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RepoModule() {
    @Provides @Singleton fun provideDatabase(context: Context): GbDatabase = GbDatabase.getInstance(context)
    @Provides @Singleton fun provideUserRepo(gbDatabase: GbDatabase): UserDao = gbDatabase.userDao()
    @Provides @Singleton fun providePlantRepo(gbDatabase: GbDatabase): PlantDao = gbDatabase.plantDao()
    @Provides @Singleton fun provideLocationRepo(gbDatabase: GbDatabase): LocationDao = gbDatabase.locationDao()
    @Provides @Singleton fun providePhotoRepo(gbDatabase: GbDatabase): PhotoDao = gbDatabase.photoDao()
}
