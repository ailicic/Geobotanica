package com.geobotanica.geobotanica.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.geobotanica.geobotanica.GeobotanicaApplication
import com.geobotanica.geobotanica.di.components.ActivityComponent
import com.geobotanica.geobotanica.di.components.DaggerActivityComponent
import com.geobotanica.geobotanica.di.modules.ActivityModule
import com.geobotanica.geobotanica.util.Lg

abstract class BaseActivity : AppCompatActivity() {
    lateinit var activityComponent: ActivityComponent
    abstract val name: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lg.v("$name: onCreate()")

        activityComponent = DaggerActivityComponent.builder()
                .applicationComponent((application as GeobotanicaApplication).applicationComponent)
                .activityModule(ActivityModule(this))
                .build()
    }

    override fun onStart() {
        super.onStart()
        Lg.v("$name: onStart()")
    }

    override fun onResume() {
        super.onResume()
        Lg.v("$name: onResume()")
    }

    override fun onPause() {
        super.onPause()
        Lg.v("$name: onPause()")
    }


    override fun onStop() {
        super.onStop()
        Lg.v("$name: onStop()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Lg.v("$name: onDestroy()")
    }
}