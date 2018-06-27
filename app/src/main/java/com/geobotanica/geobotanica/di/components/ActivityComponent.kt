package com.geobotanica.geobotanica.di.components

import com.geobotanica.geobotanica.di.PerActivity
import com.geobotanica.geobotanica.di.modules.ActivityModule
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.GpsCompoundView
import com.geobotanica.geobotanica.ui.addmeasurement.AddMeasurementsActivity
import com.geobotanica.geobotanica.ui.map.MapActivity
import com.geobotanica.geobotanica.ui.map.MapFragment
import com.geobotanica.geobotanica.ui.plantdetail.PlantDetailActivity
import dagger.Component

@PerActivity
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [ActivityModule::class]
)
interface ActivityComponent {
    fun inject(activity: MapActivity)
    fun inject(fragment: BaseFragment)
    fun inject(gpsViewGroup: GpsCompoundView)
    fun inject(mapFragment: MapFragment)
    fun inject(addMeasurementsActivity: AddMeasurementsActivity)
    fun inject(plantDetail: PlantDetailActivity)

}
