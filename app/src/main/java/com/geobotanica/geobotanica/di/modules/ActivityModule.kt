package com.geobotanica.geobotanica.di.modules

import android.content.Context
import android.location.LocationManager
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.di.PerActivity
import com.geobotanica.geobotanica.ui.BaseActivity
import dagger.Module
import dagger.Provides

@Module
class ActivityModule(private val activity: BaseActivity) {
    @Provides @PerActivity fun provideActivity(): BaseActivity = activity
}