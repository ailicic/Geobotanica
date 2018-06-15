package com.geobotanica.geobotanica.ui

import android.content.Context
import android.content.SharedPreferences
import android.support.v4.app.Fragment
import com.geobotanica.geobotanica.data.GbDatabase
import javax.inject.Inject

open class BaseFragment : Fragment() {
    @Inject lateinit var appContext: Context
    @Inject lateinit var activity: BaseActivity
    @Inject lateinit var sharedPrefs: SharedPreferences
    @Inject lateinit var gbDatabase: GbDatabase

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (getActivity() as BaseActivity).activityComponent.inject(this)
    }
}