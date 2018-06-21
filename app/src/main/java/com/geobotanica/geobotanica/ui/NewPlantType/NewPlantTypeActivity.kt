package com.geobotanica.geobotanica.ui.NewPlantType

import android.os.Bundle
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.ui.BaseActivity
import javax.inject.Inject

class NewPlantTypeActivity : BaseActivity() {
    @Inject lateinit var locationService: LocationService

    override val name = this.javaClass.name.substringAfterLast('.')

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        activityComponent.inject(this)
        setContentView(R.layout.activity_new_plant_type)
    }

//    override fun onStart() {
//        super.onStart()
//
//    }
//
//    override fun onStop() {
//        super.onStop()
//    }
}
