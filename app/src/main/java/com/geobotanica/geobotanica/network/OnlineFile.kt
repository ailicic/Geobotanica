package com.geobotanica.geobotanica.network

import kotlin.math.roundToLong

enum class OnlineFileIndex {
    MAPS_LIST, WORLD_MAP, PLANT_NAMES
}

// TODO: Get from API
val onlineFileList = listOf(
        OnlineFile(
                "Maps list",
                "http://people.okanagan.bc.ca/ailicic/Maps/maps.json.gz",
                "maps.json",
                "",
                false,
                3_359,
                35_972
        ),
        OnlineFile(
                "World map",
                "http://people.okanagan.bc.ca/ailicic/Maps/world.map.gz",
                "world.map",
                "maps",
                false,
                2_715_512,
                3_276_950
        ),
        OnlineFile(
                "Plant name database",
                "http://people.okanagan.bc.ca/ailicic/Markers/taxa.db.gz",
                "taxa.db",
                "databases",
                true,
                29_038_255,
                129_412_096
        )
)

data class OnlineFile(
        val description: String,
        val url: String,
        val fileName: String,
        val relativePath: String,
        val isInternalStorage: Boolean,
        val compressedSize: Long,
        val decompressedSize: Long
) {
    val descriptionWithSize: String
        get() = "$description (${(compressedSize.toFloat() / 1024 / 1024).roundToLong()} MB)"

    val fileNameGzip: String
        get() = "$fileName.gz"
}