package com.geobotanica.geobotanica.viewmodel

import androidx.room.withTransaction
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.location.Location
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.entity.PlantMeasurement.Type.*
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.ui.newplantconfirm.NewPlantConfirmViewModel
import com.geobotanica.geobotanica.ui.viewpager.PhotoData
import com.geobotanica.geobotanica.util.Measurement
import com.geobotanica.geobotanica.util.MockkUtil.coVerifyOne
import com.geobotanica.geobotanica.util.MockkUtil.mockkObserver
import com.geobotanica.geobotanica.util.MockkUtil.mockkRoomStatic
import com.geobotanica.geobotanica.util.MockkUtil.verifyOne
import com.geobotanica.geobotanica.util.SpekExt.allowLiveData
import com.geobotanica.geobotanica.util.SpekExt.beforeEachBlockingTest
import com.geobotanica.geobotanica.util.SpekExt.mockTime
import com.geobotanica.geobotanica.util.SpekExt.setupTestDispatchers
import io.mockk.*
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

object NewPlantConfirmViewModelTest : Spek({
    mockTime()
    allowLiveData()
    val testDispatchers = setupTestDispatchers()

    val commonNameObserver = mockkObserver<String>()
    val scientificNameObserver = mockkObserver<String>()
    val heightObserver = mockkObserver<Measurement>()
    val diameterObserver = mockkObserver<Measurement>()
    val trunkDiameterObserver = mockkObserver<Measurement>()
    val photoDataObserver = mockkObserver<List<PhotoData>>()
    val startPhotoIntentObserver = mockkObserver<File>()
    val showPhotoDeletedToastObserver = mockkObserver<Unit>()

    val photoFilename = "photo.jpg"
    val photoFile = File(photoFilename)

    val storageHelper = mockk<StorageHelper> {
        every { createPhotoFile() } returns photoFile
        every { getAbsolutePath(any()) } returns photoFilename
        every { deleteFile(any()) } returns true
    }
    val userRepo = mockk<UserRepo> { coEvery { get(any<Long>()) } returns User("user") }
    val plantRepo = mockk<PlantRepo> { coEvery { insert(any()) } returns 10L }
    val plantLocationRepo = mockk<PlantLocationRepo> { coEvery { insert(any()) } returns 0L }
    val plantPhotoRepo = mockk<PlantPhotoRepo> { coEvery { insert(any()) } returns 0L }
    val plantMeasurementRepo = mockk<PlantMeasurementRepo> { coEvery { insert(any()) } returns 0L }
    val taxonRepo = mockk<TaxonRepo> { coEvery { setTagged(any(), PlantNameTag.USED) } returns Unit }
    val vernacularRepo = mockk<VernacularRepo> { coEvery { setTagged(any(), PlantNameTag.USED) } returns Unit }

    mockkRoomStatic()
    val block = slot<suspend () -> Unit>()
    val database = mockk<GbDatabase> {
        coEvery {
            withTransaction(capture(block))
        } coAnswers  {
            block.captured.invoke()
        }
    }

    val newPlantConfirmViewModel by memoized {
        NewPlantConfirmViewModel(
                testDispatchers,
                database,
                storageHelper,
                userRepo,
                plantRepo,
                plantLocationRepo,
                plantPhotoRepo,
                plantMeasurementRepo,
                taxonRepo,
                vernacularRepo
        ).apply {
            commonName.observeForever(commonNameObserver)
            scientificName.observeForever(scientificNameObserver)
            height.observeForever(heightObserver)
            diameter.observeForever(diameterObserver)
            trunkDiameter.observeForever(trunkDiameterObserver)
            photoData.observeForever(photoDataObserver)
            startPhotoIntent.observeForever(startPhotoIntentObserver)
            showPhotoDeletedToast.observeForever(showPhotoDeletedToastObserver)
        }
    }

    beforeEachTest {
        clearMocks(
                commonNameObserver,
                scientificNameObserver,
                heightObserver,
                diameterObserver,
                trunkDiameterObserver,
                photoDataObserver,
                startPhotoIntentObserver,
                showPhotoDeletedToastObserver,
                storageHelper,
                plantRepo,
                plantPhotoRepo,
                plantMeasurementRepo,
                plantLocationRepo,
                answers = false // Keep stubbing, reset recorded calls
        )
    }


    describe("NewPlantConfirmViewModel") {
        beforeEachTest {
            newPlantConfirmViewModel.init(
                    0L,
                    "photoUri",
                    "common",
                    "scientific",
                    1L,
                    2L,
                    Plant.Type.SHRUB,
                    Measurement(1.0f),
                    Measurement(2.0f),
                    Measurement(3.0f)
            )
        }

        it("Should set commonName") { verifyOne { commonNameObserver.onChanged("common") } }
        it("Should set plantType") { newPlantConfirmViewModel.plantType shouldEqual Plant.Type.SHRUB }
        it("Should set height") { verifyOne { heightObserver.onChanged(Measurement(1.0f)) } }
        it("Should set diameter") { verifyOne { diameterObserver.onChanged(Measurement(2.0f)) } }
        it("Should set trunkDiameter") { verifyOne { trunkDiameterObserver.onChanged(Measurement(3.0f)) } }
        it("Should set photoData") {
            verify {
                photoDataObserver.onChanged(listOf(
                    PhotoData(Plant.Type.SHRUB, PlantPhoto.Type.COMPLETE, "photoUri", "user")
                ))
            }
        }


        context("Add photo") {
            beforeEachTest { newPlantConfirmViewModel.addPhoto(PlantPhoto.Type.FLOWER) }

            it("Should create new photo file") { verifyOne { storageHelper.createPhotoFile() } }
            it("Should start photo capture") { verifyOne { startPhotoIntentObserver.onChanged(photoFile) } }


            context("Cancel photo capture") {
                beforeEachTest { newPlantConfirmViewModel.deleteTemporaryPhoto() }
                it("Should delete temporary file") {
                    verifyOne { storageHelper.deleteFile(photoFilename) }
                }
            }

            context("Photo capture complete") {
                beforeEachTest { newPlantConfirmViewModel.onPhotoComplete(0) }

                it("Should update photoData") {
                    verify {
                        photoDataObserver.onChanged(listOf(
                                PhotoData(Plant.Type.SHRUB, PlantPhoto.Type.COMPLETE, "photoUri", "user"),
                                PhotoData(Plant.Type.SHRUB, PlantPhoto.Type.FLOWER, "photo.jpg", "user")
                        ))
                    }
                }

                context("Delete new photo") {
                    beforeEachBlockingTest(testDispatchers) { newPlantConfirmViewModel.deletePhoto(1) }

                    it("Should delete photo") { verify { storageHelper.deleteFile(photoFilename) } }
                    it("Should delete data") {
                        verify {
                            photoDataObserver.onChanged(listOf(
                                    PhotoData(Plant.Type.SHRUB, PlantPhoto.Type.COMPLETE, "photoUri", "user")
                            ))
                        }
                    }
                    it("Should show toast") { verifyOne { showPhotoDeletedToastObserver.onChanged(any()) } }
                }

                context("Cancel new plant") {
                    beforeEachTest { newPlantConfirmViewModel.deleteAllPhotos() }
                    it("Should delete all photos") {
                        verifyOrder {
                            storageHelper.deleteFile("photoUri")
                            storageHelper.deleteFile("photo.jpg")
                        }
                    }
                }
            }
        }

        context("Retake photo") {
            beforeEachTest { newPlantConfirmViewModel.retakePhoto() }
            it("Should start photo capture") { verifyOne { startPhotoIntentObserver.onChanged(photoFile) } }


            context("Photo capture complete") {
                beforeEachTest { newPlantConfirmViewModel.onPhotoComplete(0) }

                it("Should delete old photo") { verifyOne { storageHelper.deleteFile("photoUri") } }
                it("Should update photoData") {
                    verify {
                        photoDataObserver.onChanged(listOf(
                                PhotoData(Plant.Type.SHRUB, PlantPhoto.Type.COMPLETE, photoFilename, "user")
                        ))
                    }
                }
            }
        }

        context("Update measurements") {
            val newHeight = Measurement(4.0f)
            val newDiameter = Measurement(5.0f)
            val newTrunkDiameter = Measurement(6.0f)
            beforeEachTest {
                newPlantConfirmViewModel.onMeasurementsUpdated(newHeight, newDiameter, newTrunkDiameter)
            }

            it("Should update height") { verifyOne { heightObserver.onChanged(newHeight) }}
            it("Should update diameter") { verifyOne { diameterObserver.onChanged(newDiameter) }}
            it("Should update trunkDiameter") { verifyOne { trunkDiameterObserver.onChanged(newTrunkDiameter) }}
        }


        context("New plant confirmed") {
            val location by memoized { // memoized required for mocked/frozen timestamp
                Location(1.0, 2.0, 3.0, 0.1f, 10, 20)
            }

            val plant by memoized {
                Plant(0L, Plant.Type.SHRUB, "common", "scientific", 1L, 2L)
            }
            val plantPhoto by memoized { PlantPhoto(0L, 0L, PlantPhoto.Type.COMPLETE, "photoUri") }
            val heightMeasurement by memoized { PlantMeasurement(0L, 0L, HEIGHT, 1.0F) }
            val diameterMeasurement by memoized { PlantMeasurement(0L, 0L, DIAMETER, 2.0F) }
            val trunkDiameterMeasurement by memoized { PlantMeasurement(0L, 0L, TRUNK_DIAMETER, 3.0F) }
            val plantLocation by memoized { PlantLocation(0L, location) }

            beforeEachBlockingTest(testDispatchers) {
                coEvery { plantRepo.get(any()) } returns plant
                coEvery { plantLocationRepo.get(any()) } returns plantLocation
                coEvery { plantPhotoRepo.get(any()) } returns plantPhoto
                coEvery { plantMeasurementRepo.get(any()) } returns heightMeasurement
                newPlantConfirmViewModel.savePlantComposite(location)
            }

            it("Should insert plant in db") {
                coVerifyOne { plantRepo.insert(plant) }
            }

            it("Should insert photo in db") {
                coVerifyOne { plantPhotoRepo.insert(plantPhoto) }
            }

            it("Should insert measurements in db") {
                coVerifyOrder {
                    plantMeasurementRepo.insert(heightMeasurement)
                    plantMeasurementRepo.insert(diameterMeasurement)
                    plantMeasurementRepo.insert(trunkDiameterMeasurement)
                }
            }

            it("Should insert location in db") {
                coVerifyOne { plantLocationRepo.insert(plantLocation) }
            }
        }
    }
})