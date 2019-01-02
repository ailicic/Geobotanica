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
import kotlinx.android.synthetic.main.gps_compound_view.view.*
import javax.inject.Inject

// https://medium.com/@Sserra90/android-writing-a-compound-view-1eacbf1957fc

// TODO: Forbid holding location if gps is fixed, held, then unheld (location is stale/absent and imprecise at this time)
// TODO: Investigate when OnDetachedWindow() is called. Back button fires it but onStop of parent activity seems to not in NewPlant.
// TODO: Prevent gps hold, gps disable, then gps unhold switch (gps must be enabled first)

class GpsCompoundView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    @Inject lateinit var locationService: LocationService

    var currentLocation: Location? = null

    init {
        (context as MainActivity).applicationComponent.inject(this)
        inflate(getContext(), R.layout.gps_compound_view,this)
    }

    fun setLocation(location: Location) { // Only called if Location object found in Navigation arguments
        currentLocation = location
        gpsSwitch.isChecked = true
        precisionText.text = context.resources.getString(R.string.precision, location.precision)
        setSatellitesText(location.satellitesInUse ?: 0, location.satellitesVisible)
        holdText.visibility = View.VISIBLE
        gpsSwitch.visibility = View.VISIBLE
//        locationService.unsubscribe(this) // Not required since always called before onAttachedToWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
//        Lg.d("GpsCompoundView: onAttachedToWindow()")
        gpsSwitch.setOnCheckedChangeListener(::onToggleHoldPosition)
        if (!gpsSwitch.isChecked)
            locationService.subscribe(this, ::onLocation)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
//        Lg.d("GpsCompoundView: onDetachedFromWindow()")
        gpsSwitch.setOnClickListener(null)
        locationService.unsubscribe(this)
    }

    private fun onLocation(location: Location) {
        currentLocation = location.apply {
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

//            if (!isGpsEnabled()) {
//                _gpsFabIcon.value = GPS_OFF.drawable
//                showGpsRequiredSnackbar.call()
//            }
            locationService.unsubscribe(this)
        }
        else
            locationService.subscribe(this, ::onLocation)
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