package com.geobotanica.geobotanica.ui.new_plant_type

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.activity_new_plant_type.*
import javax.inject.Inject

class NewPlantTypeActivity : BaseActivity() {
    @Inject lateinit var locationService: LocationService

    override val name = this.javaClass.name.substringAfterLast('.')

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        activityComponent.inject(this)
        setContentView(R.layout.activity_new_plant_type)
    }

    override fun onStart() {
        super.onStart()

        buttonTree.setOnClickListener(::onClickListener)
        buttonShrub.setOnClickListener(::onClickListener)
        buttonHerb.setOnClickListener(::onClickListener)
        buttonGrass.setOnClickListener(::onClickListener)
        buttonVine.setOnClickListener(::onClickListener)
    }

    fun onClickListener(view: View): Unit {
        var message = ""
        when(view) {
            buttonTree -> message = "Tree"
            buttonShrub -> message = "Shrub"
            buttonHerb -> message = "Herb"
            buttonGrass -> message = "Grass"
            buttonVine -> message = "Vine"
        }
        Lg.d("onClickListener(): Clicked $message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
