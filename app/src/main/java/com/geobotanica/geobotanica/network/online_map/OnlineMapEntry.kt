package com.geobotanica.geobotanica.network.online_map

import com.geobotanica.geobotanica.util.capitalizeWords
import com.geobotanica.geobotanica.util.replacePrefix
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
        get() {
            var string = name
                    .removeSuffix(".map")
                    .removeSuffix(">") // Present if name is too long on scraped Mapsforge website
                    .replace('-', ' ')
                    .replacePrefix("us", "US")
                    .capitalizeWords()
            return if (isFolder)
                string
            else
                "$string ($size)"
        }

}