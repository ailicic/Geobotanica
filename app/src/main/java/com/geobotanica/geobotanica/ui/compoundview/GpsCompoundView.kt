package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.ui.MainActivity
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.compound_gps.view.*
import javax.inject.Inject

// https://medium.com/@Sserra90/android-writing-a-compound-view-1eacbf1957fc

// TODO: Investigate when onDetachedFromWindow() is called. Back button fires it but onStop of parent activity seems to not in NewPlant.
// TODO: Prevent gps hold, gps disable, then gps unhold switch (gps must be enabled first)

class GpsCompoundView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    @Inject lateinit var locationService: LocationService

    var currentLocation: Location? = null
    val activity: MainActivity = context as MainActivity

    init {
//        Lg.d("GpsCompoundView: init{}")
        activity.applicationComponent.inject(this)
        inflate(getContext(), R.layout.compound_gps,this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
//        Lg.d("GpsCompoundView: onAttachedToWindow()")

        if (activity.currentLocation != null)
            importLocationData(activity.currentLocation!!)
        else
            locationService.subscribe(this, ::onLocation)
        gpsSwitch.setOnCheckedChangeListener(::onToggleHoldPosition)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
//        Lg.d("GpsCompoundView: onDetachedFromWindow()")
        gpsSwitch.setOnCheckedChangeListener(null)
        locationService.unsubscribe(this)
    }

    private fun importLocationData(location: Location) {
        currentLocation = location
        gpsSwitch.isChecked = true
        precisionText.text = context.resources.getString(R.string.precision, location.precision)
        setSatellitesText(location.satellitesInUse ?: 0, location.satellitesVisible)
        holdText.visibility = View.VISIBLE
        gpsSwitch.visibility = View.VISIBLE
    }

    private fun onLocation(location: Location) {
        currentLocation = location
        location.run {
//            Lg.v("onLocation(): $this")
            precision?.let {
                precisionText.text = context.resources.getString(R.string.precision, precision)
                holdText.visibility = View.VISIBLE
                gpsSwitch.visibility = View.VISIBLE
            }
            satellitesInUse?.let { setSatellitesText(satellitesInUse, satellitesVisible) }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onToggleHoldPosition(buttonView: CompoundButton, isChecked: Boolean) {
        Lg.d("onToggleHoldPosition(): isChecked=$isChecked")
        if (isChecked) {
            locationService.unsubscribe(this)
            activity.currentLocation = currentLocation

//            if (!isGpsEnabled()) {
//                _gpsFabIcon.value = GPS_OFF.drawable
//                showGpsRequiredSnackbar.call()
//            }
        }
        else {
            activity.currentLocation = null
            locationService.subscribe(this, ::onLocation)
        }
    }

    private fun setSatellitesText(satellitesInUse: Int, satellitesVisible: Int) {
        satellitesText.text = context.resources.getString(R.string.satellites, satellitesInUse, satellitesVisible)
    }
}


// EXAMPLE OF RX -> LIVEDATA CONVERSION IN VIEWMODEL
// https://github.com/googlesamples/android-architecture-components/issues/41
//class UserListViewModel @Inject
//constructor(@NonNull userRepository: UserRepository) : ViewModel() {
//
//    internal val userList: LiveData<Resource<List<User>>>
//
//    init {
//        userList = LiveDataReactiveStreams.fromPublisher(userRepository
//                .getUserList()
//                .subscribeOn(Schedulers.newThread())
//                .observeOn(AndroidSchedulers.mainThread()))
//    }
//}