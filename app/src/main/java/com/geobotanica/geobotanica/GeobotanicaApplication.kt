package com.geobotanica.geobotanica

import android.app.Application
import com.geobotanica.geobotanica.di.components.ApplicationComponent
import com.geobotanica.geobotanica.di.components.DaggerApplicationComponent
import com.geobotanica.geobotanica.di.modules.ApplicationModule
import com.geobotanica.geobotanica.util.Emulator
import com.geobotanica.geobotanica.util.Lg

class GeobotanicaApplication : Application() {
    lateinit var applicationComponent: ApplicationComponent

    override fun onCreate() {
        super.onCreate()
        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(this))
                .build()
        applicationComponent.inject(this)

        if (Emulator.isEmulator())
            Lg.d("Running on emulator")
        else
            Lg.d("Running on device")
    }
}