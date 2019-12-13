package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.FragmentActivity
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.ui.dialog.ItemListDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton


class PlantTypeButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    private lateinit var currentPlantType: Plant.Type
    lateinit var onNewPlantType: (Plant.Type) -> Unit

    init {
        setOnClickListener { showPlantTypeDialog() }
    }

    fun init(PlantType: Plant.Type) = updatePlantType(PlantType)

    private fun updatePlantType(plantType: Plant.Type) {
        currentPlantType = plantType
        val plantTypeDrawables = resources.obtainTypedArray(R.array.plant_type_drawable_array)
        setImageResource(plantTypeDrawables.getResourceId(plantType.ordinal, -1))
        plantTypeDrawables.recycle()
    }

    private fun showPlantTypeDialog() {
        ItemListDialog(
                titleResId = R.string.change_plant_type,
                drawableArrayResId = R.array.plant_type_drawable_array,
                enumValues = Plant.Type.values().filter { it != currentPlantType },
                onItemSelected = ::onNewPlantTypeSelected
        ).show((context as FragmentActivity).supportFragmentManager,"tag")
    }

    private fun onNewPlantTypeSelected(plantType: Plant.Type) {
        updatePlantType(plantType)
        onNewPlantType(plantType)
    }
}