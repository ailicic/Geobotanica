package com.geobotanica.geobotanica.di.components

import android.app.Application
import android.content.Context
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.di.modules.ApplicationModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class])
interface ApplicationComponent {
    fun inject(application: Application)

    //Expose to sub-components
    fun context(): Context
}
