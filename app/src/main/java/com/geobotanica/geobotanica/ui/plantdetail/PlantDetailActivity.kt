package com.geobotanica.geobotanica.ui.plantdetail

import android.os.Bundle
import android.support.design.widget.Snackbar
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.ui.BaseActivity
import kotlinx.android.synthetic.main.activity_plant_detail.*

class PlantDetailActivity : BaseActivity() {
    override val name = this.javaClass.name.substringAfterLast('.')

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_detail)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Add new photos/measurements", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }
}