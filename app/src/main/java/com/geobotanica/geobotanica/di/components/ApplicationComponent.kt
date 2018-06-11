package com.geobotanica.geobotanica.di.components

import com.geobotanica.geobotanica.GeobotanicaApplication
import com.geobotanica.geobotanica.di.modules.ApplicationModule
import com.geobotanica.geobotanica.ui.new_record.NewRecordFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
//        modules = [ApplicationModule::class, AndroidModule::class]
    modules = [ApplicationModule::class]
)
interface ApplicationComponent {
    fun inject(application: GeobotanicaApplication)
    fun inject(newRecordFragment: NewRecordFragment)
}
