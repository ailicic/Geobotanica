package com.geobotanica.geobotanica.di.components

import com.geobotanica.geobotanica.di.PerActivity
import com.geobotanica.geobotanica.di.modules.ActivityModule
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.compoundview.GpsCompoundView
import com.geobotanica.geobotanica.ui.addmeasurement.AddMeasurementsActivity
import com.geobotanica.geobotanica.ui.MainActivity
import com.geobotanica.geobotanica.ui.fragment.MapFragment
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
    fun inject(addMeasurementsActivity: AddMeasurementsActivity)
    fun inject(plantDetailFragment: PlantDetailFragment)
}
