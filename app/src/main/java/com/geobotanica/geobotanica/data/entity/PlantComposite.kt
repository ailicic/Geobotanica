package com.geobotanica.geobotanica.data.entity

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Relation

class PlantComposite {
    @Embedded lateinit var plant: Plant

    @Relation(parentColumn = "id", entityColumn = "plantId")
    var locations: List<PlantLocation> = emptyList()

    @Relation(parentColumn = "id", entityColumn = "plantId")
    var photos: List<Photo> = emptyList()

    @Relation(parentColumn = "id", entityColumn = "plantId")
    var measurements: List<Measurement> = emptyList()

    override fun toString(): String {
        val sb = StringBuilder("\n\n$plant (id=${plant.id})")
        locations.forEach { sb.append("\n$it (id=${it.id})") }
        photos.forEach { sb.append("\n$it (id=${it.id})") }
        measurements.forEach { sb.append("\n$it (id=${it.id})") }
        return sb.toString()
    }
}