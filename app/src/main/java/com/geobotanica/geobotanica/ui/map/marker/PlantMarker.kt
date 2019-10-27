package com.geobotanica.geobotanica.ui.map.marker

import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.entity.Plant.Type.*
import com.geobotanica.geobotanica.ui.MainActivity
import com.geobotanica.geobotanica.ui.map.GbMapView
import com.geobotanica.geobotanica.ui.map.PlantMarkerData
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.setScaledBitmap
import kotlinx.android.synthetic.main.marker_bubble.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.util.MapViewProjection
import org.mapsforge.map.view.InputListener
import javax.inject.Inject

@Suppress("DEPRECATION")
class PlantMarker(
        val plantMarkerData: PlantMarkerData,
        private val activity: MainActivity,
        private val mapView: GbMapView,
        private val onPlantMarkerLongPress: (Long) -> Unit
) : GbMarker(
        activity.resources.getDrawable(when (plantMarkerData.plantType!!) {
            // TODO: Use drawable array + enum ordinal instead?
            TREE -> R.drawable.marker_purple
            SHRUB -> R.drawable.marker_blue
            HERB -> R.drawable.marker_green
            GRASS -> R.drawable.marker_light_green
            VINE -> R.drawable.marker_yellow
            FUNGUS -> R.drawable.marker_yellow // TODO: Use different color
        })
) {
    @Inject lateinit var storageHelper: StorageHelper

    val plantId: Long = plantMarkerData.plantId

    private var markerBubble: View? = null
    val isShowingMarkerBubble
        get() = markerBubble != null

    private val zoomListener = object : InputListener {
        override fun onZoomEvent() {
            markerBubble?.run {
                Lg.d("PlantMarker: onZoomEvent()")
                GlobalScope.launch(Dispatchers.Main) { // Hack to ensure call to getBubbleLatitudeOffset() occurs AFTER marker bitmap is resized.
                    delay(200)
                    while (mapView.model.mapViewPosition.animationInProgress()) { delay(50); Lg.v("PlantMarker: Waiting 50 ms for animation to finish...") }
                    delay(50)
                    layoutParams = getBubbleLayoutParams()
                }
            }
        }

        override fun onMoveEvent() { }
    }

    init {
        activity.applicationComponent.inject(this)
        latLong = LatLong(plantMarkerData.latitude!!, plantMarkerData.longitude!!)

        onPress = {
            if (markerBubble == null) {
                mapView.hideAnyMarkerInfoBubble()
                showInfoBubble()
            } else
                hideInfoBubble()
        }
        onLongPress = { onPlantMarkerLongPress(plantId) }
    }

    override fun toString(): String = plantMarkerData.toString()

    fun hideInfoBubble() {
        markerBubble?.run {
            mapView.removeView(markerBubble)
            markerBubble = null
            deregisterZoomListener()
        }
    }

    private fun showInfoBubble() {
        val plantTypeDrawables = activity.resources.obtainTypedArray(R.array.plant_type_drawable_array)
        val plantTypeIconResId = plantTypeDrawables.getResourceId(plantMarkerData.plantType!!.ordinal, -1)
        plantTypeDrawables.recycle()

        val photoPath = storageHelper.photoUriFrom(plantMarkerData.photoFilename!!)
        Lg.d("photoPath = $photoPath")
        markerBubble = LayoutInflater.from(activity).inflate(R.layout.marker_bubble, mapView, false).apply {
            plantPhoto.doOnPreDraw { plantPhoto.setScaledBitmap(photoPath) }
            plantTypeIcon.setImageResource(plantTypeIconResId)
            plantMarkerData.commonName?.let { commonNameText.text = it; commonNameText.isVisible = true }
            plantMarkerData.scientificName?.let { scientificNameText.text = it; scientificNameText.isVisible = true }
            setOnClickListener { hideInfoBubble() }
        }
        mapView.addView(markerBubble, getBubbleLayoutParams())
        registerZoomListener()
    }

    private fun registerZoomListener() = mapView.addInputListener(zoomListener)
    private fun deregisterZoomListener() = mapView.removeInputListener(zoomListener)

    private fun getBubbleLayoutParams(): MapView.LayoutParams {
        return MapView.LayoutParams(
                MapView.LayoutParams.WRAP_CONTENT,
                MapView.LayoutParams.WRAP_CONTENT,
                LatLong(plantMarkerData.latitude!! + getBubbleLatitudeOffset(), plantMarkerData.longitude!!),
                MapView.LayoutParams.Alignment.BOTTOM_CENTER)
    }

    private fun getBubbleLatitudeOffset(): Double {
        val markerHeight = bitmap.height.toDouble()

        val projection = MapViewProjection(mapView)
        val latitudeSpan = projection.latitudeSpan

        val displayMetrics = DisplayMetrics().also { activity.windowManager.defaultDisplay.getMetrics(it) }
        val screenHeight = displayMetrics.heightPixels.toDouble()

        return markerHeight * latitudeSpan / screenHeight * 1.2
    }
}