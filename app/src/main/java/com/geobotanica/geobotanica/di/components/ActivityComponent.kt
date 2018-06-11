package com.geobotanica.geobotanica.di.components

import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.di.PerActivity
import com.geobotanica.geobotanica.di.modules.ActivityModule
import com.geobotanica.geobotanica.ui.new_record.NewRecordFragment
import dagger.Component

@PerActivity
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [ActivityModule::class]
//    modules = [ActivityModule::class, AndroidModule::class]
)
interface ActivityComponent {
//    fun inject(baseActivity: BaseActivity)
    fun inject(newRecordFragment: NewRecordFragment)

    //Expose to sub-components (if user sub-component is used)
//    fun activity(): BaseActivity
}
