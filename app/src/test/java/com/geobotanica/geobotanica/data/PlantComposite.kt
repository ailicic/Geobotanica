package com.geobotanica.geobotanica.data

import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.ui.map.marker.PlantMarkerData
import com.geobotanica.geobotanica.util.SpekLiveData.describeWithLiveData
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.threeten.bp.OffsetDateTime

// TODO: Plant Composite test has lost value. Prob better to test only location and photo sorting

//class MeasurementTests : Spek({
//
//    describeWithLiveData("Plant Composite") {
//        val plantComposite = PlantComposite().apply {
//            plant = Plant(1L, Plant.Type.TREE, "common", "latin", 1L, 1L, OffsetDateTime.MAX)
//            plant.id = 1L
//            plantLocations = listOf(
//                    PlantLocation(1L, Location(1.0, 2.0, 3.0,1.0f,
//                            10, 20, OffsetDateTime.MAX))
//            )
//            plantPhotos = listOf(
//                    PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE,"photoFilename", OffsetDateTime.MAX)
//            )
//            plantMeasurements = listOf(
//                    PlantMeasurement(1L, 1L, PlantMeasurement.Type.HEIGHT,5.0f, OffsetDateTime.MAX),
//                    PlantMeasurement(1L, 1L, PlantMeasurement.Type.DIAMETER,0.5f, OffsetDateTime.MAX)
//            )
//        }
//
//
//        context("When converted to Plant Marker Data") {
//
//            val plantMarkerData = PlantMarkerData(plantComposite)
//            it("Should be correct") {
//                plantMarkerData shouldEqual listOf(PlantMarkerData(
//                        1L, Plant.Type.TREE,"common", "latin",
//                        1.0, 2.0, "photoFilename",
//                        OffsetDateTime.MAX.toString().substringBefore('T')
//                ))
//            }
//        }
//    }
//})