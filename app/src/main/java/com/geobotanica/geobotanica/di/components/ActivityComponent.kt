package com.geobotanica.geobotanica.di.components

import com.geobotanica.geobotanica.di.PerActivity
import com.geobotanica.geobotanica.di.modules.ActivityModule
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.compoundview.GpsCompoundView
import com.geobotanica.geobotanica.ui.MainActivity
import com.geobotanica.geobotanica.ui.map.MapFragment
import com.geobotanica.geobotanica.ui.newplantmeasurement.NewPlantMeasurementFragment
import com.geobotanica.geobotanica.ui.plantdetail.PlantDetailFragment
import dagger.Component

@PerActivity
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [ActivityModule::class]
)
interface ActivityComponent {
    fun inject(activity: MainActivity)
    fun inject(fragment: BaseFragment)
    fun inject(gpsViewGroup: GpsCompoundView)
    fun inject(mapFragment: MapFragment)
    fun inject(newPlantMeasurementFragment: NewPlantMeasurementFragment)
    fun inject(plantDetailFragment: PlantDetailFragment)
}
