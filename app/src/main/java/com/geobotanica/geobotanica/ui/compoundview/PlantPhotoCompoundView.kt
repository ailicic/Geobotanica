package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import com.geobotanica.geobotanica.data.entity.PlantPhoto.Type.*
import com.geobotanica.geobotanica.util.ImageViewExt.setScaledBitmap
import com.geobotanica.geobotanica.util.SingleLiveEvent
import kotlinx.android.synthetic.main.plant_photo_compound_view.view.*


class PlantPhotoCompoundView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val editPhoto = SingleLiveEvent<PlantPhoto.Type>()

    init {
        inflate(getContext(), R.layout.plant_photo_compound_view,this)
        photoEditButton.setOnClickListener { editPhoto.value = photoType }
    }

    var photoType: PlantPhoto.Type = COMPLETE
        set(photoType) {
            field = photoType

            when (photoType) {
                LEAF -> photoTypeIcon.setImageResource(R.drawable.photo_type_leaf)
                FLOWER -> photoTypeIcon.setImageResource(R.drawable.photo_type_flower)
                FRUIT -> photoTypeIcon.setImageResource(R.drawable.photo_type_fruit)
                TRUNK -> photoTypeIcon.setImageResource(R.drawable.photo_type_trunk)
                COMPLETE -> photoTypeIcon.setImageResource(R.drawable.photo_type_complete)
            }
            if (photoType == COMPLETE)
                plantPhoto.scaleType = ImageView.ScaleType.FIT_CENTER
            else
                plantPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
        }

    fun setPhoto(photoUri: String) {
        plantPhoto.setScaledBitmap(photoUri)
        isVisible = true
    }
}