package com.geobotanica.geobotanica

import android.app.Application
import com.geobotanica.geobotanica.di.components.ApplicationComponent
import com.geobotanica.geobotanica.di.components.DaggerApplicationComponent
import com.geobotanica.geobotanica.di.modules.ApplicationModule

class GeobotanicaApplication : Application() {
    lateinit var applicationComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()
        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(this))
//                .androidModule(AndroidModule(this))
                .build()
        applicationComponent.inject(this)
    }
}