package com.geobotanica.geobotanica.di.modules

import com.geobotanica.geobotanica.di.PerActivity
import com.geobotanica.geobotanica.ui.BaseActivity
import dagger.Module
import dagger.Provides

@Module
class ActivityModule(private val activity: BaseActivity) {
    @Provides @PerActivity fun provideActivity(): BaseActivity = activity
}