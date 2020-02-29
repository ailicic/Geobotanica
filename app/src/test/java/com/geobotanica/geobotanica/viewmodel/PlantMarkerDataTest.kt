package com.geobotanica.geobotanica.viewmodel

import com.geobotanica.geobotanica.android.location.Location
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.ui.map.marker.PlantMarkerData
import com.geobotanica.geobotanica.util.GbTime
import com.geobotanica.geobotanica.util.toDateString
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PlantMarkerDataTest : Spek({
    val sooner = GbTime.now()
    val later = sooner.plusMillis(1)

    describe("Converted from PlantMarkerComposite") {

        val plantComposite = PlantComposite().apply {
            plant = Plant(1L,
                    Plant.Type.TREE,
                    "common",
                    "scientific",
                    1L,
                    1L,
                    later,
                    1L
            )
            plantLocations = listOf(
                    PlantLocation(1L, Location(1.0, 2.0, 3.0, 1.0f, 10, 20, sooner)),
                    PlantLocation(2L, Location(4.0, 5.0, 6.0, 0.1f, 15, 22, later))
            )
            plantPhotos = listOf(
                    PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo2", later),
                    PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo1", sooner)
            )
            plantMeasurements = listOf(
                    PlantMeasurement(1L, 1L, PlantMeasurement.Type.HEIGHT, 1.0f, later),
                    PlantMeasurement(1L, 1L, PlantMeasurement.Type.DIAMETER, 1.0f, later),
                    PlantMeasurement(1L, 1L, PlantMeasurement.Type.HEIGHT, 1.0f, sooner),
                    PlantMeasurement(1L, 1L, PlantMeasurement.Type.DIAMETER, 1.0f, sooner)
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
                    later.toDateString()
            )
        }
    }

    describe("Main photo selection") {
        context("List containing two complete photos") {
            val photos = listOf(
                    PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo2", later),
                    PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo1", sooner)
            )
            it("Should select newer") {
                photos.selectMain() shouldEqual PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo2", later)
            }
        }
        context("List containing one older complete and one newer leaf photo") {
            val photos = listOf(
                    PlantPhoto(1L, 1L, PlantPhoto.Type.LEAF, "photo2", later),
                    PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo1", sooner)
            )
            it("Should select complete photo") {
                photos.selectMain() shouldEqual PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE, "photo1", sooner)
            }
        }
        context("List containing two flower photos") {
            val photos = listOf(
                    PlantPhoto(1L, 1L, PlantPhoto.Type.FLOWER, "photo2", later),
                    PlantPhoto(1L, 1L, PlantPhoto.Type.FLOWER, "photo1", sooner)
            )
            it("Should select newer photo") {
                photos.selectMain() shouldEqual PlantPhoto(1L, 1L, PlantPhoto.Type.FLOWER, "photo2", later)
            }
        }
    }
})
