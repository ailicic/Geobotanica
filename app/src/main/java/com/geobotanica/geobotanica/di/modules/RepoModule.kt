package com.geobotanica.geobotanica.di.modules

import android.content.Context
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.dao.*
import com.geobotanica.geobotanica.data_ro.PlantDatabaseRo
import com.geobotanica.geobotanica.data_ro.dao.TaxonCompositeDao
import com.geobotanica.geobotanica.data_ro.dao.TaxonDao
import com.geobotanica.geobotanica.data_ro.dao.VernacularDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RepoModule {
    @Provides @Singleton fun provideGbDatabase(context: Context): GbDatabase = GbDatabase.getInstance(context)
    @Provides @Singleton fun provideUserDao(gbDatabase: GbDatabase): UserDao = gbDatabase.userDao()
    @Provides @Singleton fun providePlantDao(gbDatabase: GbDatabase): PlantDao = gbDatabase.plantDao()
    @Provides @Singleton fun providePlantCompositeDao(gbDatabase: GbDatabase): PlantCompositeDao = gbDatabase.plantCompositeDao()
    @Provides @Singleton fun providePlantLocationDao(gbDatabase: GbDatabase): PlantLocationDao = gbDatabase.plantLocationDao()
    @Provides @Singleton fun providePhotoDao(gbDatabase: GbDatabase): PlantPhotoDao = gbDatabase.photoDao()
    @Provides @Singleton fun provideMeasurementDao(gbDatabase: GbDatabase): PlantMeasurementDao = gbDatabase.measurementDao()

    @Provides @Singleton fun providePlantDatabaseRo(context: Context): PlantDatabaseRo = PlantDatabaseRo.getInstance(context)
    @Provides @Singleton fun provideTaxonDao(plantDatabaseRo: PlantDatabaseRo): TaxonDao = plantDatabaseRo.taxonDao()
    @Provides @Singleton fun provideTaxonCompositeDao(plantDatabaseRo: PlantDatabaseRo): TaxonCompositeDao =
            plantDatabaseRo.taxonCompositeDao()
    @Provides @Singleton fun provideVernacularDao(plantDatabaseRo: PlantDatabaseRo): VernacularDao = plantDatabaseRo.vernacularDao()
}
