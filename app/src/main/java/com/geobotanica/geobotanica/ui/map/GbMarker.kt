package com.geobotanica.geobotanica.ui.map

import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.util.Lg
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


class GbMarker(val activity: AppCompatActivity, val plantId: Long, map: MapView): Marker(map) {

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
}