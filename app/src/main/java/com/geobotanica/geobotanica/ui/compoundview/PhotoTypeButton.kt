package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import com.geobotanica.geobotanica.ui.dialog.ItemListDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton


class PhotoTypeButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    lateinit var onNewPhotoType: (PlantPhoto.Type) -> Unit
    private lateinit var currentPhotoType: PlantPhoto.Type
    private lateinit var currentPlantType: Plant.Type

    init {
        setOnClickListener { showPhotoTypeDialog() }
    }

    fun init(initialPhotoType: PlantPhoto.Type) {
        updatePhotoTypeIcon(initialPhotoType)
    }

    val plantTypeObserver = Observer<Plant.Type> {
        currentPlantType = it
        val validPhotoTypes = PlantPhoto.typesValidFor(it)
        if (! validPhotoTypes.contains(currentPhotoType))
            onNewPhotoTypeSelected(PlantPhoto.Type.COMPLETE) // Default to complete if current type invalid
        isEnabled = validPhotoTypes.size > 1
    }

    private fun updatePhotoTypeIcon(photoType: PlantPhoto.Type) {
        currentPhotoType = photoType
        val photoTypeDrawables = resources.obtainTypedArray(R.array.photo_type_drawable_array)
        setImageResource(photoTypeDrawables.getResourceId(photoType.ordinal, -1))
        photoTypeDrawables.recycle()
    }

    private fun showPhotoTypeDialog() {
        ItemListDialog(
                titleResId = R.string.change_photo_type,
                drawableArrayResId = R.array.photo_type_drawable_array,
                enumValues = PlantPhoto.typesValidFor(currentPlantType).filter { it != currentPhotoType },
                onItemSelected = ::onNewPhotoTypeSelected
        ).show((context as FragmentActivity).supportFragmentManager,"tag")
    }

    private fun onNewPhotoTypeSelected(photoType: PlantPhoto.Type) {
        updatePhotoTypeIcon(photoType)
        onNewPhotoType(photoType)
    }
}