package com.geobotanica.geobotanica.ui.viewpager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.setScaledBitmap
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.plant_photo_compound_view.view.*


class PlantPhotoAdapter(
        private val onClickPhoto: () -> Unit,
        private val onClickPhotoType: () -> Unit,
        private val onClickDeletePhoto: () -> Unit,
        private val onClickRetakePhoto: () -> Unit,
        private val onClickAddPhoto: () -> Unit
) : RecyclerView.Adapter<PlantPhotoAdapter.ViewHolder>() {

    var items: List<PhotoData> = emptyList()
    lateinit var currentViewHolder: ViewHolder

    var isPhotoMenuVisible = false
        set(value) {
            field = value
            updatePhotoMenuButtons()
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.plant_photo_compound_view, parent, false)
        return ViewHolder(view)
    }

    @Suppress("DEPRECATION")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        currentViewHolder = holder
        val item = items[position]

        val resources = holder.view.context.resources

        Lg.d("onBindViewHolder() $item")
        holder.plantPhoto.doOnPreDraw {
            holder.plantPhoto.setScaledBitmap(item.photoUri)
        }

        val photoTypeDrawables = resources.obtainTypedArray(R.array.photo_type_drawable_array)
        holder.changePhotoTypeButton.setImageResource(photoTypeDrawables.getResourceId(item.photoType.ordinal, -1))
        photoTypeDrawables.recycle()

//        holder.userTimestampText.text = "Photo taken by ${item.user} on ${item.timestamp}"

        updatePhotoMenuButtons()

        with(holder) {
            photoMenuButton.setOnClickListener { isPhotoMenuVisible = true }
            plantPhoto.setOnClickListener { onClickPhoto() }
            changePhotoTypeButton.setOnClickListener { onClickPhotoType() }
            deletePhotoButton.setOnClickListener { onClickDeletePhoto() }
            retakePhotoButton.setOnClickListener { onClickRetakePhoto() }
            addPhotoButton.setOnClickListener { onClickAddPhoto() }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun updatePhotoMenuButtons() {
        with(currentViewHolder) {
            photoMenuButton.isVisible = ! isPhotoMenuVisible
            deletePhotoButton.isVisible = isPhotoMenuVisible && itemCount > 1
            retakePhotoButton.isVisible = isPhotoMenuVisible
            addPhotoButton.isVisible = isPhotoMenuVisible
        }
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val plantPhoto: ImageView = view.plantPhoto
        val photoMenuButton: FloatingActionButton = view.photoMenuButton
        val changePhotoTypeButton: FloatingActionButton = view.changePhotoTypeButton
        val deletePhotoButton: FloatingActionButton = view.deletePhotoButton
        val retakePhotoButton: FloatingActionButton = view.retakePhotoButton
        val addPhotoButton: FloatingActionButton = view.addPhotoButton
//        val userTimestampText: TextView = view.userTimestampText
    }
}

data class PhotoData(
        var photoType: PlantPhoto.Type,
        var photoUri: String
//        val user: String,
//        val timestamp: String
)