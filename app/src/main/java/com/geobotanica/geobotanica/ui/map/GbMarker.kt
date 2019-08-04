package com.geobotanica.geobotanica.ui.map

import android.graphics.drawable.Drawable
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.overlay.Marker

/**
 * Wrapper class to simplify subclassing Mapsforge Marker class
 */
open class GbMarker(
        drawable: Drawable,
        private val onPress: (() -> Unit)? = null,
        private val onLongPress: (() -> Unit)? = null
): Marker(
        LatLong(0.0, 0.0),
        AndroidGraphicFactory.convertToBitmap(drawable),
        0, 0
) {
    init {
        verticalOffset = (-1)  * bitmap.height / 2
    }

    override fun onTap(tapLatLong: LatLong?, layerXY: Point?, tapXY: Point?): Boolean {
        if (contains(layerXY, tapXY)) {
            onPress?.invoke()
            return true
        }
        return false
    }

    override fun onLongPress(tapLatLong: LatLong?, layerXY: Point?, tapXY: Point?): Boolean {
        if (contains(layerXY, tapXY)) {
            onLongPress?.invoke()
            return true
        }
        return false
    }
}