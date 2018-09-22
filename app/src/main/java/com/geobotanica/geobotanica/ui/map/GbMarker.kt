package com.geobotanica.geobotanica.ui.map

import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.util.Lg
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


class GbMarker(plantMarkerData: PlantMarkerData, val activity: AppCompatActivity, map: MapView): Marker(map) {
    val plantId: Long = plantMarkerData.plantId

    init {
        plantMarkerData.let {
            title = it.commonName
            snippet = it.latinName
            subDescription = it.dateCreated
            @Suppress("DEPRECATION")
            icon = activity.resources.getDrawable(it.icon!!)
            position.setCoords(it.latitude!!, it.longitude!!)
            image = Drawable.createFromPath(it.photoPath)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

    }

    override fun onLongPress(event: MotionEvent?, mapView: MapView?): Boolean {
        val touched = hitTest(event, mapView)
        if (touched) {
            Lg.d("Opening plant detail: id=$plantId")
            val bundle = bundleOf("plantId" to plantId)
            val navController = activity.findNavController(R.id.fragment)
            navController.navigate(R.id.plantDetailFragment, bundle)
        }
        return touched
    }

    override fun onMarkerClickDefault(marker: Marker?, mapView: MapView?): Boolean {
        if (marker!!.isInfoWindowOpen)
            marker.closeInfoWindow()
        else
            marker.showInfoWindow()
        return true
    }

}