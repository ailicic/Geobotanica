package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.FragmentActivity
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import com.geobotanica.geobotanica.ui.dialog.ItemListDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton


class AddPhotoButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    private lateinit var plantType: Plant.Type
    private lateinit var onAddPhoto: (PlantPhoto.Type) -> Unit

    init {
        setOnClickListener { showPhotoTypeDialog() }
    }

    fun init(plantType: Plant.Type, onAddPhoto: (PlantPhoto.Type) -> Unit) {
        this.plantType = plantType
        this.onAddPhoto = onAddPhoto
    }

    private fun showPhotoTypeDialog() {
        ItemListDialog(
                titleResId = R.string.select_new_photo_type,
                drawableArrayResId = R.array.photo_type_drawable_array,
                enumValues = PlantPhoto.typesValidFor(plantType),
                onItemSelected = onAddPhoto
        ).show((context as FragmentActivity).supportFragmentManager,"tag")
    }
}