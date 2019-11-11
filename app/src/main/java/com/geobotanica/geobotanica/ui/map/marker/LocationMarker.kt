package com.geobotanica.geobotanica.ui.map.marker

import android.graphics.drawable.Drawable
import com.geobotanica.geobotanica.android.location.Location
import com.geobotanica.geobotanica.ui.map.GbMapView
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SingleLiveEvent

class LocationMarker(
        drawable: Drawable,
        mapView: GbMapView
) : GbMarker(
        drawable,
        onLongPress = { Lg.d("LocationMarker long tap") }
) {
    init {
        mapView.layerManager.layers.add(this)
        onPress = { showLocationMarkerToast.call() }
    }

    val showLocationMarkerToast = SingleLiveEvent<Unit>()

    fun updateLocation(location: Location) {
        latLong = location.toLatLong()
    }
}