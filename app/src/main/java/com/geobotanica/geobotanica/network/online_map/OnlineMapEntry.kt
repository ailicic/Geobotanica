package com.geobotanica.geobotanica.network.online_map

import com.geobotanica.geobotanica.util.capitalizeWords
import com.squareup.moshi.JsonClass


interface OnlineMapEntry // Used by Moshi PolymorphicJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class OnlineMapFile(
        val url: String,
        val size: String
) : OnlineMapEntry {

    val name: String
        get() = url.substringAfterLast('/')

    val printName: String
        get() = name
                .removeSuffix(".map")
                .removeSuffix(">") // Present if url is too long
                .replace('-', ' ')
                .capitalizeWords()
}

@JsonClass(generateAdapter = true)
data class OnlineMapFolder(
        val url: String,
        val contents: MutableList<OnlineMapEntry> = mutableListOf()
) : OnlineMapEntry {

    val name: String
        get() = url
                .substringAfterLast('/')

    val printName: String
        get() = name
                .substringAfterLast('/')
                .replace('-', ' ')
                .capitalizeWords()
}


// (OLD) MOSHI WARNING: Must use var for member variables in non-data classes,
// otherwise they are ignored during JSON serialization.
// https://github.com/square/moshi/issues/315#issuecomment-371978007