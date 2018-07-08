package com.geobotanica.geobotanica.ui.newplanttype

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.newplantphoto.NewPlantPhotoActivity
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.activity_new_plant_type.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*

// TODO: Use Toolbar instead of ActionBar
class NewPlantTypeActivity : BaseActivity() {
    override val name = this.javaClass.name.substringAfterLast('.')

    private var userId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_plant_type)
        userId = intent.getLongExtra(getString(R.string.extra_user_id), -1L)
        Lg.d("Intent extras: userId=$userId")
    }

    override fun onStart() {
        super.onStart()

        buttonTree.setOnClickListener(::onClickListener)
        buttonShrub.setOnClickListener(::onClickListener)
        buttonHerb.setOnClickListener(::onClickListener)
        buttonGrass.setOnClickListener(::onClickListener)
        buttonVine.setOnClickListener(::onClickListener)
    }

    private fun onClickListener(view: View) {
        var plantType = Plant.Type.TREE
        when(view) {
            buttonTree -> plantType = Plant.Type.TREE
            buttonShrub -> plantType = Plant.Type.SHRUB
            buttonHerb -> plantType = Plant.Type.HERB
            buttonGrass -> plantType = Plant.Type.GRASS
            buttonVine -> plantType = Plant.Type.VINE
        }
        Lg.d("onClickListener(): Clicked $plantType")
        val intent = Intent(this, NewPlantPhotoActivity::class.java)
                .putExtra(getString(R.string.extra_user_id), userId)
                .putExtra(getString(R.string.extra_plant_type), plantType.ordinal)
        if (gps.gpsSwitch.isChecked)
            intent.putExtra(getString(R.string.extra_location), gps.currentLocation)
        startActivity(intent)
        finish()
    }
}
