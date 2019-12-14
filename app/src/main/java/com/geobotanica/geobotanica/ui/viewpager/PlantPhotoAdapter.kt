package com.geobotanica.geobotanica.ui.viewpager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import com.geobotanica.geobotanica.ui.viewpager.PlantPhotoAdapter.PhotoViewHolder
import com.geobotanica.geobotanica.util.GbTime
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.setScaledBitmap
import com.geobotanica.geobotanica.util.toDateString
import kotlinx.android.synthetic.main.compound_plant_photo.view.*


class PlantPhotoAdapter(
        private val onClickPhoto: () -> Unit,
        private val onClickDeletePhoto: () -> Unit,
        private val onClickRetakePhoto: () -> Unit,
        private val onClickAddPhoto: (PlantPhoto.Type) -> Unit,
        private val onNewPhotoType: (PlantPhoto.Type) -> Unit
) : ListAdapter<PhotoData, PhotoViewHolder>(PhotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return PhotoViewHolder(inflater.inflate(R.layout.compound_plant_photo, parent, false))
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(
                getItem(position),
                onClickPhoto,
                onClickDeletePhoto,
                onClickRetakePhoto,
                onClickAddPhoto,
                onNewPhotoType
        )
    }

    inner class PhotoViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        fun bind(
                photoData: PhotoData,
                onClickPhoto: () -> Unit,
                onClickDeletePhoto: () -> Unit,
                onClickRetakePhoto: () -> Unit,
                onClickAddPhoto: (photoType: PlantPhoto.Type) -> Unit,
                onNewPhotoType: (PlantPhoto.Type) -> Unit
        ) {
            Lg.v("PlantPhotoAdapter.bindTo() #$adapterPosition $photoData")
            updatePhotoMenuVisibility(false)

            with(view) {
                plantPhoto.doOnPreDraw {
                    plantPhoto.setScaledBitmap(photoData.photoUri)
                }

                plantPhoto.setOnClickListener { onClickPhoto() }

                changePhotoTypeButton.init(photoData.plantType, photoData.photoType) { onNewPhotoType(it) }

                deletePhotoButton.onDeletePhoto = {
                    updatePhotoMenuVisibility(false)
                    onClickDeletePhoto()
                }

                retakePhotoButton.onRetakePhoto = {
                    updatePhotoMenuVisibility(false)
                    onClickRetakePhoto()
                }


                addPhotoButton.init(photoData.plantType) {
                    updatePhotoMenuVisibility(false)
                    onClickAddPhoto(it)
                }

                overflowButton.setOnClickListener { updatePhotoMenuVisibility(true) }

                userTimestampText.text = context.resources.getString(R.string.taken_by, photoData.userNickname, photoData.timestamp)
            }
        }

        private fun updatePhotoMenuVisibility(isVisible: Boolean) = with(view) {
            overflowButton.isVisible = ! isVisible
            deletePhotoButton.isVisible = isVisible && itemCount > 1
            retakePhotoButton.isVisible = isVisible
            addPhotoButton.isVisible = isVisible
        }
    }
}

class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoData>() {
    override fun areItemsTheSame(oldItem: PhotoData, newItem: PhotoData): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: PhotoData, newItem: PhotoData): Boolean = oldItem == newItem
}

data class PhotoData(
        val plantType: Plant.Type,
        val photoType: PlantPhoto.Type,
        val photoUri: String,
        val userNickname: String,
        val timestamp: String = GbTime.now().toDateString(),
        val id: Long = 0L // Not used unless the PlantPhoto exists in the db and id has been generated
)