package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
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

    lateinit var onAddPhoto: (PlantPhoto.Type) -> Unit
    lateinit var plantType: LiveData<Plant.Type>

    init {
        setOnClickListener { showPhotoTypeDialog() }
    }

    private fun showPhotoTypeDialog() {
        ItemListDialog(
                titleResId = R.string.select_new_photo_type,
                drawableArrayResId = R.array.photo_type_drawable_array,
                enumValues = PlantPhoto.typesValidFor(plantType.value!!),
                onItemSelected = onAddPhoto
        ).show((context as FragmentActivity).supportFragmentManager,"tag")
    }
}