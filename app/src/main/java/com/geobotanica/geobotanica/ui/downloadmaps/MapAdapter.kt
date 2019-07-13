package com.geobotanica.geobotanica.ui.downloadmaps

import androidx.recyclerview.widget.DiffUtil
import com.geobotanica.geobotanica.network.online_map.OnlineMapEntry

class MapDiffCallback : DiffUtil.ItemCallback<OnlineMapEntry>() {

    override fun areItemsTheSame(oldItem: OnlineMapEntry, newItem: OnlineMapEntry): Boolean =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: OnlineMapEntry, newItem: OnlineMapEntry): Boolean =
        oldItem == newItem
}