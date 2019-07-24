package com.geobotanica.geobotanica.di.components

import com.geobotanica.geobotanica.di.modules.ApplicationModule
import com.geobotanica.geobotanica.di.modules.RepoModule
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.MainActivity
import com.geobotanica.geobotanica.ui.compoundview.GpsCompoundView
import com.geobotanica.geobotanica.ui.downloadassets.DownloadAssetsFragment
import com.geobotanica.geobotanica.ui.downloadmaps.DownloadMapsFragment
import com.geobotanica.geobotanica.ui.map.MapFragment
import com.geobotanica.geobotanica.ui.newplantconfirm.NewPlantConfirmFragment
import com.geobotanica.geobotanica.ui.newplantmeasurement.NewPlantMeasurementFragment
import com.geobotanica.geobotanica.ui.newplantname.NewPlantNameFragment
import com.geobotanica.geobotanica.ui.newplantphoto.NewPlantPhotoFragment
import com.geobotanica.geobotanica.ui.newplanttype.NewPlantTypeFragment
import com.geobotanica.geobotanica.ui.plantdetail.PlantDetailFragment
import com.geobotanica.geobotanica.ui.searchplantname.SearchPlantNameFragment
import dagger.Component
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class, RepoModule::class] )
interface ApplicationComponent {
    fun inject(mainActivity: MainActivity)
    fun inject(baseFragment: BaseFragment)

    fun inject(mapFragment: MapFragment)
    fun inject(newPlantTypeFragment: NewPlantTypeFragment)
    fun inject(newPlantPhotoFragment: NewPlantPhotoFragment)
    fun inject(searchPlantNameFragment: SearchPlantNameFragment)
    fun inject(newPlantNameFragment: NewPlantNameFragment)
    fun inject(newPlantMeasurementFragment: NewPlantMeasurementFragment)
    fun inject(newPlantConfirmFragment: NewPlantConfirmFragment)
    fun inject(plantDetailFragment: PlantDetailFragment)

    fun inject(downloadAssetsFragment: DownloadAssetsFragment)
    fun inject(downloadMapsFragment: DownloadMapsFragment)

    fun inject(gpsCompoundView: GpsCompoundView)
}
