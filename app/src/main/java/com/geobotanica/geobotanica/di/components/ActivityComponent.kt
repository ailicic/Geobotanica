package com.geobotanica.geobotanica.di.components

import com.geobotanica.geobotanica.di.PerActivity
import com.geobotanica.geobotanica.di.modules.ActivityModule
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.new_record.NewRecordFragment
import dagger.Component

@PerActivity
@Component(
    dependencies = [ApplicationComponent::class],
    modules = [ActivityModule::class]
)
interface ActivityComponent {
    fun inject(fragment: BaseFragment)
    fun inject(newRecordFragment: NewRecordFragment)
}
