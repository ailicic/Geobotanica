package com.geobotanica.geobotanica.ui.map.marker

import com.geobotanica.geobotanica.data.entity.*


data class PlantMarkerData(
        val plantId: Long,
        val plantType: Plant.Type? = null,
        val commonName: String? = null,
        val scientificName: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val photoFilename: String? = null,
        val dateCreated: String? = null
) {

    constructor(plantComposite: PlantComposite): this(
            0L,
            plantComposite.plant.type,
            plantComposite.plant.commonName,
            plantComposite.plant.scientificName,
            plantComposite.plantLocations.mostRecent().latitude,
            plantComposite.plantLocations.mostRecent().longitude,
            plantComposite.plantPhotos.selectMain().filename,
            plantComposite.plant.timestamp.toSimpleDate()
    )
}