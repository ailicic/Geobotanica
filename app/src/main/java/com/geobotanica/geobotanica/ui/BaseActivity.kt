package com.geobotanica.geobotanica.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.geobotanica.geobotanica.GeobotanicaApplication
import com.geobotanica.geobotanica.di.components.ActivityComponent
import com.geobotanica.geobotanica.di.components.DaggerActivityComponent
import com.geobotanica.geobotanica.di.modules.ActivityModule

open class BaseActivity : AppCompatActivity() {
    lateinit var activityComponent: ActivityComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent = DaggerActivityComponent.builder()
                .applicationComponent((application as GeobotanicaApplication).applicationComponent)
                .activityModule(ActivityModule(this))
                .build()
    }
}