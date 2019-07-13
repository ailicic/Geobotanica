package com.geobotanica.geobotanica.ui.downloadmaps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.network.online_map.OnlineMapEntry
import kotlinx.android.synthetic.main.list_item.view.*

class MapListAdapter(private val onClick: (OnlineMapEntry) -> Unit) :
        ListAdapter<OnlineMapEntry, MapViewHolder>(MapDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MapViewHolder(inflater.inflate(R.layout.list_item, parent, false))
    }


    override fun onBindViewHolder(holder: MapViewHolder, position: Int) {
        holder.bindTo(getItem(position), onClick)
    }
}

class MapViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    fun bindTo(onlineMapEntry: OnlineMapEntry, onClick: (OnlineMapEntry) -> Unit) {
        if (onlineMapEntry.isFolder)
            view.icon.setImageResource(R.drawable.ic_folder_24dp)
        else
            view.icon.setImageResource(R.drawable.ic_file_download_24dp)

        view.text.text = onlineMapEntry.printName
        view.setOnClickListener { onClick(onlineMapEntry) }
    }
}

class MapDiffCallback : DiffUtil.ItemCallback<OnlineMapEntry>() {

    override fun areItemsTheSame(oldItem: OnlineMapEntry, newItem: OnlineMapEntry): Boolean =
        oldItem.url == newItem.url

    override fun areContentsTheSame(oldItem: OnlineMapEntry, newItem: OnlineMapEntry): Boolean =
        oldItem == newItem
}