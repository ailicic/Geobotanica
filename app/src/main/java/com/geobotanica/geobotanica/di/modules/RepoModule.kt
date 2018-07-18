package com.geobotanica.geobotanica.di.modules

import android.content.Context
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.dao.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RepoModule {
    @Provides @Singleton fun provideDatabase(context: Context): GbDatabase = GbDatabase.getInstance(context)
    @Provides @Singleton fun provideUserDao(gbDatabase: GbDatabase): UserDao = gbDatabase.userDao()
    @Provides @Singleton fun providePlantDao(gbDatabase: GbDatabase): PlantDao = gbDatabase.plantDao()
    @Provides @Singleton fun providePlantCompositeDao(gbDatabase: GbDatabase): PlantCompositeDao = gbDatabase.plantCompositeDao()
    @Provides @Singleton fun providePlantLocationDao(gbDatabase: GbDatabase): PlantLocationDao = gbDatabase.plantLocationDao()
    @Provides @Singleton fun providePhotoDao(gbDatabase: GbDatabase): PhotoDao = gbDatabase.photoDao()
    @Provides @Singleton fun provideMeasurementDao(gbDatabase: GbDatabase): MeasurementDao = gbDatabase.measurementDao()
}
