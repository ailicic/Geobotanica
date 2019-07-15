package com.geobotanica.geobotanica.network.online_map

import com.geobotanica.geobotanica.util.capitalizeWords
import com.geobotanica.geobotanica.util.replacePrefix
import com.squareup.moshi.JsonClass

// Note: Removed File/Folder subclasses to have simpler single data class with equals() for MapDiffCallback

@JsonClass(generateAdapter = true)
data class OnlineMapEntry(
        val isFolder: Boolean,
        val url: String,
        val size: String = "", // Used only if isFolder = false
        val contents: MutableList<OnlineMapEntry> = mutableListOf() // Used only if isFolder = true
) {
    val filename: String
        get() = url.substringAfterLast('/')

    val printName: String
        get() {
            var string = filename
                    .removeSuffix(".map")
                    .removeSuffix(">") // Present if filename is too long on scraped Mapsforge website
                    .replace('-', ' ')
                    .replacePrefix("us", "US")
                    .capitalizeWords()
            return if (isFolder)
                string
            else
                "$string ($size)"
        }

}