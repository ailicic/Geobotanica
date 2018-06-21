package com.geobotanica.geobotanica.ui

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.widget.CompoundButton
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.gps_compound_view.view.*
import javax.inject.Inject

// https://medium.com/@Sserra90/android-writing-a-compound-view-1eacbf1957fc

// TODO: Try JVM overloads
class GpsCompoundView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    @Inject lateinit var locationService: LocationService

    var currentLocation: Location? = null
    var isPositionHeld: Boolean = false
        get() = gpsSwitch.isSelected

    init {
        Lg.d("GpsCompoundView()")
        (context as BaseActivity).activityComponent.inject(this)
        inflate(getContext(), R.layout.gps_compound_view,this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Lg.d("GpsCompoundView: onAttachedToWindow()")
        locationService.subscribe(::onLocation)
        gpsSwitch.setOnCheckedChangeListener(::onToggleHoldPosition)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Lg.d("GpsCompoundView: onDetachedFromWindow()")
        gpsSwitch.setOnClickListener(null)
        locationService.unsubscribe(::onLocation)
    }

    private fun onLocation(location: Location) {
        currentLocation = location
        with(location) {
            Lg.d("onLocation(): $this")

            precision?.let {
                precisionText.text = context.resources.getString(R.string.precision, precision)
                gpsSwitch.isEnabled = true
            }
            satellitesInUse?.let { satellitesText?.text = context.resources.getString(R.string.satellites, satellitesInUse, satellitesVisible) }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onToggleHoldPosition(buttonView: CompoundButton, isChecked: Boolean) {
        Lg.d("onToggleHoldPosition(): isChecked=$isChecked")
        if (isChecked)
            locationService.unsubscribe(::onLocation)
        else
            locationService.subscribe(::onLocation)
    }
}