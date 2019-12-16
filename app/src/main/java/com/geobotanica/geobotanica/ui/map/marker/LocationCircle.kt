package com.geobotanica.geobotanica.ui.map.marker

import com.geobotanica.geobotanica.android.location.Location
import com.geobotanica.geobotanica.ui.map.GbMapView
import org.mapsforge.core.graphics.Color
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.overlay.Circle


class LocationCircle(
        mapView: GbMapView
) : Circle(
        LatLong(0.0, 0.0),
        0f,
        AndroidGraphicFactory.INSTANCE.createPaint().apply {
            setStyle(Style.FILL)
            color = AndroidGraphicFactory.INSTANCE.createColor(64, 0, 0, 0)
        },
        AndroidGraphicFactory.INSTANCE.createPaint().apply {
            setStyle(Style.STROKE)
            color = AndroidGraphicFactory.INSTANCE.createColor(Color.BLACK)
            strokeWidth = 2f
        }, true
) {
    init {
        mapView.layerManager.layers.add(this)
    }

    fun updateLocation(location: Location) {
        setLatLong(location.toLatLong())
        location.precision?.let { radius = it }
    }
}