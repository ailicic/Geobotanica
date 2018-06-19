package com.geobotanica.geobotanica.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.geobotanica.geobotanica.util.Lg
import javax.inject.Inject

abstract class BaseFragment : Fragment() {
    @Inject lateinit var appContext: Context
    @Inject lateinit var activity: BaseActivity
    @Inject lateinit var sharedPrefs: SharedPreferences

    abstract val name: String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Lg.v("$name: onAttach()")
        (getActivity() as BaseActivity).activityComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lg.v("$name: onCreate()")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
        Lg.v("$name: onCreateView()")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Lg.v("$name: onViewCreated()")
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

    override fun onDestroyView() {
        super.onDestroyView()
        Lg.v("$name: onDestroyView()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Lg.v("$name: onDestroy()")
    }

    override fun onDetach() {
        super.onDetach()
        Lg.v("$name: onDetach()")
    }
}