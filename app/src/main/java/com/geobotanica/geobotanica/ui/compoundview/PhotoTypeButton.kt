package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.FragmentActivity
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

    private lateinit var plantType: Plant.Type
    private lateinit var photoType: PlantPhoto.Type
    private lateinit var onNewPhotoType: (PlantPhoto.Type) -> Unit

    init {
        setOnClickListener { showPhotoTypeDialog() }
    }

    fun init(
            plantType: Plant.Type,
            photoType: PlantPhoto.Type,
            onNewPhotoType: (PlantPhoto.Type) -> Unit)
    {
        updatePhotoTypeIcon(photoType)
        updatePlantType(plantType)
        this.onNewPhotoType = onNewPhotoType
    }

    private fun updatePlantType(plantType: Plant.Type) {
        this.plantType = plantType
        val validPhotoTypes = PlantPhoto.typesValidFor(plantType)
        if (! validPhotoTypes.contains(photoType))
            onNewPhotoTypeSelected(PlantPhoto.Type.COMPLETE) // Default to complete if current type invalid
        isEnabled = validPhotoTypes.size > 1
    }

    private fun updatePhotoTypeIcon(photoType: PlantPhoto.Type) {
        this.photoType = photoType
        val photoTypeDrawables = resources.obtainTypedArray(R.array.photo_type_drawable_array)
        setImageResource(photoTypeDrawables.getResourceId(photoType.ordinal, -1))
        photoTypeDrawables.recycle()
    }

    private fun showPhotoTypeDialog() {
        ItemListDialog(
                titleResId = R.string.change_photo_type,
                drawableArrayResId = R.array.photo_type_drawable_array,
                enumValues = PlantPhoto.typesValidFor(plantType).filter { it != photoType },
                onItemSelected = ::onNewPhotoTypeSelected
        ).show((context as FragmentActivity).supportFragmentManager,"tag")
    }

    private fun onNewPhotoTypeSelected(photoType: PlantPhoto.Type) {
        updatePhotoTypeIcon(photoType)
        onNewPhotoType(photoType)
    }
}