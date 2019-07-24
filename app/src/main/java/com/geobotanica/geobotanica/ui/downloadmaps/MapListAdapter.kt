package com.geobotanica.geobotanica.ui.downloadmaps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DOWNLOADED
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.NOT_DOWNLOADED
import kotlinx.android.synthetic.main.map_list_item.view.*

class MapListAdapter(
        private val onClickFolder: (OnlineMapListItem) -> Unit,
        private val onClickDownload: (OnlineMapListItem) -> Unit,
        private val onClickCancel: (OnlineMapListItem) -> Unit,
        private val onClickDelete: (OnlineMapListItem) -> Unit
    ) : ListAdapter<OnlineMapListItem, MapViewHolder>(MapDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MapViewHolder(inflater.inflate(R.layout.map_list_item, parent, false))
    }


    override fun onBindViewHolder(holder: MapViewHolder, position: Int) {
        holder.bindTo(getItem(position), onClickFolder, onClickDownload, onClickCancel, onClickDelete)
    }
}

class MapViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    fun bindTo(
            mapListItem: OnlineMapListItem,
            onClickFolder: (OnlineMapListItem) -> Unit,
            onClickDownload: (OnlineMapListItem) -> Unit,
            onClickCancel: (OnlineMapListItem) -> Unit,
            onClickDelete: (OnlineMapListItem) -> Unit
    ) {
        view.icon.isVisible = false
        view.cancelButton.isVisible = false
        view.deleteButton.isVisible = false
        view.progressBar.isVisible = false
        view.setOnClickListener(null) // Side effect: view.isClickable = true
        view.isClickable = false

        if (mapListItem.isFolder) {
            view.icon.setImageResource(R.drawable.ic_folder_24dp)
            view.icon.isVisible = true
            view.setOnClickListener { onClickFolder(mapListItem) }
        } else {
            if (mapListItem.status == NOT_DOWNLOADED) {
                view.icon.setImageResource(R.drawable.ic_file_download_24dp)
                view.icon.isVisible = true
                view.setOnClickListener{ onClickDownload(mapListItem) }
            } else if (mapListItem.isDownloading) {
                view.progressBar.isVisible = true
                view.cancelButton.isVisible = true
                view.cancelButton.setOnClickListener { onClickCancel(mapListItem) }
            } else if (mapListItem.status == DOWNLOADED) {
                view.icon.setImageResource(R.drawable.ic_done_black_24dp)
                view.icon.isVisible = true
                view.deleteButton.isVisible = true
                view.deleteButton.setOnClickListener { onClickDelete(mapListItem) }
            }
        }
        view.text.text = mapListItem.printName
    }
}

class MapDiffCallback : DiffUtil.ItemCallback<OnlineMapListItem>() {

    override fun areItemsTheSame(oldItem: OnlineMapListItem, newItem: OnlineMapListItem): Boolean =
        oldItem.id == newItem.id && oldItem.isFolder == newItem.isFolder

    override fun areContentsTheSame(oldItem: OnlineMapListItem, newItem: OnlineMapListItem): Boolean =
        oldItem == newItem
}


data class OnlineMapListItem(
        val id: Long,
        val isFolder: Boolean,
        val printName: String,
        val status: Long = NOT_DOWNLOADED // Relevant only if isFolder = false
) {
    val isDownloading = status > 0L
}

fun OnlineMapFolder.toListItem(): OnlineMapListItem = OnlineMapListItem(id,true, printName)
fun OnlineMap.toListItem(): OnlineMapListItem = OnlineMapListItem(id,false, printName, status)