package com.geobotanica.geobotanica.viewmodel

import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.ui.map.marker.PlantMarkerData
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.threeten.bp.OffsetDateTime

class PlantMarkerDataTest : Spek({
    describe("Converted from PlantMarkerComposite") {

        val plantComposite = PlantComposite().apply {
            plant = Plant(1L,
                    Plant.Type.TREE,
                    "common",
                    "scientific",
                    1L,
                    1L,
                    OffsetDateTime.MAX
            ).apply{
                id = 1L
            }
            plantLocations = listOf(
                    PlantLocation(1L, Location(1.0, 2.0, 3.0, 1.0f, 10, 20, OffsetDateTime.MIN)),
                    PlantLocation(2L, Location(4.0, 5.0, 6.0, 0.1f, 15, 22, OffsetDateTime.MAX))
            )
            plantPhotos = listOf(
                    PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo2", OffsetDateTime.MAX),
                    PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo1", OffsetDateTime.MIN)
            )
            plantMeasurements = listOf(
                    PlantMeasurement(1L, 1L, PlantMeasurement.Type.HEIGHT, 1.0f, OffsetDateTime.MAX),
                    PlantMeasurement(1L, 1L, PlantMeasurement.Type.DIAMETER, 1.0f, OffsetDateTime.MAX),
                    PlantMeasurement(1L, 1L, PlantMeasurement.Type.HEIGHT, 1.0f, OffsetDateTime.MIN),
                    PlantMeasurement(1L, 1L, PlantMeasurement.Type.DIAMETER, 1.0f, OffsetDateTime.MIN)
            )
        }

        val plantMarkerData = PlantMarkerData(plantComposite)

        it("Should convert correctly") {
            plantMarkerData shouldEqual PlantMarkerData(
                    1L,
                    Plant.Type.TREE,
                    "common",
                    "scientific",
                    4.0,
                    5.0,
                    "photo2",
                    OffsetDateTime.MAX.toSimpleDate()
            )
        }
    }

    describe("Main photo selection") {
        context("List containing two complete photos") {
            val photos = listOf(
                    PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo2", OffsetDateTime.MAX),
                    PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo1", OffsetDateTime.MIN)
            )
            it("Should select newer") {
                photos.selectMain() shouldEqual PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo2", OffsetDateTime.MAX)
            }
        }
        context("List containing one older complete and one newer leaf photo") {
            val photos = listOf(
                    PlantPhoto(1L, 1L, PlantPhoto.Type.LEAF, "photo2", OffsetDateTime.MAX),
                    PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo1", OffsetDateTime.MIN)
            )
            it("Should select complete photo") {
                photos.selectMain() shouldEqual PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo1", OffsetDateTime.MIN)
            }
        }
        context("List containing two flower photos") {
            val photos = listOf(
                    PlantPhoto(1L, 1L, PlantPhoto.Type.FLOWER, "photo2", OffsetDateTime.MAX),
                    PlantPhoto(1L, 1L, PlantPhoto.Type.FLOWER, "photo1", OffsetDateTime.MIN)
            )
            it("Should select newer photo") {
                photos.selectMain() shouldEqual PlantPhoto(1L, 1L, PlantPhoto.Type.FLOWER, "photo2", OffsetDateTime.MAX)
            }
        }
    }
})
