package com.geobotanica.geobotanica.ui.viewpager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import com.geobotanica.geobotanica.ui.compoundview.AddPhotoButton
import com.geobotanica.geobotanica.ui.compoundview.DeletePhotoButton
import com.geobotanica.geobotanica.ui.compoundview.PhotoTypeButton
import com.geobotanica.geobotanica.ui.compoundview.RetakePhotoButton
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.setScaledBitmap
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.compound_plant_photo.view.*


class PlantPhotoAdapter(
        private val onClickPhoto: () -> Unit,
        private val onDeletePhoto: () -> Unit,
        private val onRetakePhoto: () -> Unit,
        private val onAddPhoto: (photoType: PlantPhoto.Type) -> Unit,
        private val plantType: LiveData<Plant.Type>
) : RecyclerView.Adapter<PlantPhotoAdapter.ViewHolder>() {

    lateinit var items: List<PhotoData>
    private lateinit var currentViewHolder: ViewHolder

    private var isPhotoMenuVisible = false
        set(value) {
            field = value
            updatePhotoMenuButtons()
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.compound_plant_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        currentViewHolder = holder
        val item = items[position]
        Lg.v("PlantPhotoAdapter.onBindViewHolder() $item")

        with(holder) {
            plantPhoto.doOnPreDraw {
                plantPhoto.setScaledBitmap(item.photoUri)
            }

            plantPhoto.setOnClickListener { onClickPhoto() }

            changePhotoTypeButton.init(item.photoType, plantType)
            changePhotoTypeButton.onNewPhotoType = {
                items[position].photoType = it
            }

            deletePhotoButton.onDeletePhoto = {
                isPhotoMenuVisible = false
                onDeletePhoto()
            }

            retakePhotoButton.onRetakePhoto = {
                isPhotoMenuVisible = false
                onRetakePhoto()
            }

            addPhotoButton.plantType = plantType
            addPhotoButton.onAddPhoto = {
                isPhotoMenuVisible = false
                onAddPhoto(it)
            }

            photoMenuButton.setOnClickListener { isPhotoMenuVisible = true }

//        userTimestampText.text = "Photo taken by ${item.user} on ${item.timestamp}"
        }
        updatePhotoMenuButtons()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.changePhotoTypeButton.removeObserver()
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
        val changePhotoTypeButton: PhotoTypeButton = view.changePhotoTypeButton
        val deletePhotoButton: DeletePhotoButton = view.deletePhotoButton
        val retakePhotoButton: RetakePhotoButton = view.retakePhotoButton
        val addPhotoButton: AddPhotoButton = view.addPhotoButton
//        val userTimestampText: TextView = view.userTimestampText
    }
}

data class PhotoData(
        var photoType: PlantPhoto.Type,
        var photoUri: String
//        val user: String,
//        val timestamp: String
)