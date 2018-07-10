package com.geobotanica.geobotanica.di.modules

import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.ui.plantdetail.PlantDetailViewModelFactory
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ViewModelModule {
    @Provides @Singleton
    fun providePlantDetailViewModelFactory(
            userRepo: UserRepo,
            plantRepo: PlantRepo,
            locationRepo: LocationRepo,
            photoRepo: PhotoRepo,
            measurementRepo: MeasurementRepo
    ): PlantDetailViewModelFactory
    {
        return PlantDetailViewModelFactory(userRepo, plantRepo,  locationRepo, photoRepo, measurementRepo)
    }
}
