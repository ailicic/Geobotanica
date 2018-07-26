package com.geobotanica.geobotanica.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.geobotanica.geobotanica.util.Lg
import javax.inject.Inject

abstract class BaseFragment : Fragment() {
    @Inject lateinit var appContext: Context
    @Inject lateinit var activity: MainActivity
    @Inject lateinit var defaultSharedPrefs: SharedPreferences

    protected abstract val className: String
    abstract val sharedPrefsKey: String

    // NavBundle/SharedPrefs keys
    protected val userIdKey = "userId"
    protected val plantIdKey = "plantId"
    protected val plantTypeKey = "plantType"
    protected val commonNameKey = "commonName"
    protected val latinNameKey = "plantLatinName"
    protected val photoUriKey = "plantPhoto"
    protected val locationKey = "location"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Lg.v("$className: onAttach()")
        (context as MainActivity).applicationComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lg.v("$className: onCreate()")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Lg.v("$className: onCreateView()")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Lg.v("$className: onViewCreated()")
    }

    override fun onStart() {
        super.onStart()
        Lg.v("$className: onStart()")
    }

    override fun onResume() {
        super.onResume()
        Lg.v("$className: onResume()")
    }

    override fun onPause() {
        super.onPause()
        Lg.v("$className: onPause()")
    }

    override fun onStop() {
        super.onStop()
        Lg.v("$className: onStop()")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Lg.v("$className: onDestroyView()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Lg.v("$className: onDestroy()")
    }

    override fun onDetach() {
        super.onDetach()
        Lg.v("$className: onDetach()")
    }
}