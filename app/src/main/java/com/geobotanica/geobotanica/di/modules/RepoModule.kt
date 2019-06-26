package com.geobotanica.geobotanica.di.modules

import android.content.Context
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.dao.*
import com.geobotanica.geobotanica.data_taxa.TaxaDatabase
import com.geobotanica.geobotanica.data_taxa.dao.*
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RepoModule {
    @Provides @Singleton fun provideGbDatabase(appContext: Context): GbDatabase = GbDatabase.getInstance(appContext)
    @Provides @Singleton fun provideUserDao(gbDatabase: GbDatabase): UserDao = gbDatabase.userDao()
    @Provides @Singleton fun providePlantDao(gbDatabase: GbDatabase): PlantDao = gbDatabase.plantDao()
    @Provides @Singleton fun providePlantCompositeDao(gbDatabase: GbDatabase): PlantCompositeDao = gbDatabase.plantCompositeDao()
    @Provides @Singleton fun providePlantLocationDao(gbDatabase: GbDatabase): PlantLocationDao = gbDatabase.plantLocationDao()
    @Provides @Singleton fun providePhotoDao(gbDatabase: GbDatabase): PlantPhotoDao = gbDatabase.photoDao()
    @Provides @Singleton fun provideMeasurementDao(gbDatabase: GbDatabase): PlantMeasurementDao = gbDatabase.measurementDao()

    @Provides @Singleton fun provideTaxaDatabase(context: Context): TaxaDatabase = TaxaDatabase.getInstance(context)
    @Provides @Singleton fun provideTaxonDao(taxaDatabase: TaxaDatabase): TaxonDao = taxaDatabase.taxonDao()
    @Provides @Singleton fun provideVernacularDao(taxaDatabase: TaxaDatabase): VernacularDao = taxaDatabase.vernacularDao()
    @Provides @Singleton fun provideTagDao(taxaDatabase: TaxaDatabase): TagDao = taxaDatabase.tagDao()
    @Provides @Singleton fun provideTypeDao(taxaDatabase: TaxaDatabase): TypeDao = taxaDatabase.typeDao()

    @Provides @Singleton fun providePlantNameSearchService(
            taxonRepo: TaxonRepo,
            vernacularRepo: VernacularRepo
    ): PlantNameSearchService = PlantNameSearchService(taxonRepo, vernacularRepo)
}
