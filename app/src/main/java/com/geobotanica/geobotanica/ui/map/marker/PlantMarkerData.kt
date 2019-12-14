package com.geobotanica.geobotanica.ui.map.marker

import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.util.GbTime
import com.geobotanica.geobotanica.util.toDateString


data class PlantMarkerData(
        val plantId: Long,
        val plantType: Plant.Type? = null,
        val commonName: String? = null,
        val scientificName: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val photoFilename: String? = null,
        val dateCreated: String? = GbTime.now().toDateString()
) {

    constructor(plantComposite: PlantComposite): this(
            plantId = plantComposite.plant.id,
            plantType = plantComposite.plant.type,
            commonName = plantComposite.plant.commonName,
            scientificName = plantComposite.plant.scientificName,
            latitude = plantComposite.plantLocations.mostRecent.latitude,
            longitude = plantComposite.plantLocations.mostRecent.longitude,
            photoFilename = plantComposite.plantPhotos.selectMain().filename,
            dateCreated = plantComposite.plant.timestamp.toDateString()
    )
}