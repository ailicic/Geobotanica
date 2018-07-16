package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.gps_compound_view.view.*
import javax.inject.Inject

// https://medium.com/@Sserra90/android-writing-a-compound-view-1eacbf1957fc

// TODO: Investigate when OnDetachedWindow() is called. Back button fires it but onStop of parent activity seems to not in NewPlant.
class GpsCompoundView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    @Inject lateinit var locationService: LocationService

    var currentLocation: Location? = null

    init {
        Lg.v("GpsCompoundView()")
        (context as BaseActivity).activityComponent.inject(this)
        inflate(getContext(), R.layout.gps_compound_view,this)
    }

    fun setLocation(location: Location) { // Only called if Location object found in Activity intent in OnCreate()
        currentLocation = location
        gpsSwitch.isChecked = true
        precisionText.text = context.resources.getString(R.string.precision, location.precision)
        setSatellitesText(location.satellitesInUse ?: 0, location.satellitesVisible)
        holdText.visibility = View.VISIBLE
        gpsSwitch.visibility = View.VISIBLE
//        locationService.unsubscribe(context) // Not required since always called before onAttachedToWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Lg.d("GpsCompoundView: onAttachedToWindow()")
        gpsSwitch.setOnCheckedChangeListener(::onToggleHoldPosition)
        if (!gpsSwitch.isChecked) {
            locationService.subscribe(context, ::onLocation)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Lg.d("GpsCompoundView: onDetachedFromWindow()")
        gpsSwitch.setOnClickListener(null)
        locationService.unsubscribe(context)
    }

    private fun onLocation(location: Location) {
        currentLocation = location
        with(location) {
//            Lg.v("onLocation(): $this")

            precision?.let {
                precisionText.text = context.resources.getString(R.string.precision, precision)
                holdText.visibility = View.VISIBLE
                gpsSwitch.visibility = View.VISIBLE
            }
            satellitesInUse?.let { setSatellitesText(satellitesInUse ?: 0, satellitesVisible) }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onToggleHoldPosition(buttonView: CompoundButton, isChecked: Boolean) {
        Lg.d("onToggleHoldPosition(): isChecked=$isChecked")
        if (isChecked)
            locationService.unsubscribe(context)
        else
            locationService.subscribe(context, ::onLocation)
    }

    private fun setSatellitesText(satellitesInUse: Int, satellitesVisible: Int) {
        satellitesText.text = context.resources.getString(R.string.satellites, satellitesInUse, satellitesVisible)
    }
}