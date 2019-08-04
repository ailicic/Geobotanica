package com.geobotanica.geobotanica.ui.map

import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.util.Lg
import org.mapsforge.core.model.LatLong


@Suppress("DEPRECATION")
class PlantMarker(
        val plantMarkerData: PlantMarkerData,
        val activity: AppCompatActivity
) : GbMarker(
        activity.resources.getDrawable(plantMarkerData.icon!!),
        onPress = {
            Lg.d("Press $plantMarkerData")
        },
        onLongPress = {
            Lg.d("Opening plant detail: id=${plantMarkerData.plantId}")
            val bundle = bundleOf("plantId" to plantMarkerData.plantId)
            val navController = activity.findNavController(R.id.fragment)
            navController.navigate(R.id.plantDetailFragment, bundle)
        }
) {
    val plantId: Long = plantMarkerData.plantId

    init {

        latLong = LatLong(plantMarkerData.latitude!!, plantMarkerData.longitude!!)
    }

    override fun toString(): String {
        return plantMarkerData.toString()
    }
}