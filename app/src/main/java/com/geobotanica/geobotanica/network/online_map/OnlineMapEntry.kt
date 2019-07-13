package com.geobotanica.geobotanica.network.online_map

import com.geobotanica.geobotanica.util.capitalizeWords
import com.squareup.moshi.JsonClass

// Note: Removed File/Folder subclasses to have single data class with equals() for MapDiffCallback

@JsonClass(generateAdapter = true)
data class OnlineMapEntry(
        val isFolder: Boolean,
        val url: String,
        val size: String = "", // Used only if isFolder = false
        val contents: MutableList<OnlineMapEntry> = mutableListOf() // Used only if isFolder = true
) {
    val name: String
        get() = url.substringAfterLast('/')

    val printName: String
        get() = name
                .removeSuffix(".map")
                .removeSuffix(">") // Present if url is too long
                .replace('-', ' ')
                .capitalizeWords()
}