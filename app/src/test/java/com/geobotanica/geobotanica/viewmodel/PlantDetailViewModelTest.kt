package com.geobotanica.geobotanica.viewmodel

import androidx.room.withTransaction
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.location.Location
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.entity.Plant.Type.SHRUB
import com.geobotanica.geobotanica.data.entity.Plant.Type.TREE
import com.geobotanica.geobotanica.data.entity.PlantMeasurement.Type.*
import com.geobotanica.geobotanica.data.entity.PlantPhoto.Type.COMPLETE
import com.geobotanica.geobotanica.data.entity.PlantPhoto.Type.FLOWER
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.ui.plantdetail.PlantDetailViewModel
import com.geobotanica.geobotanica.ui.viewpager.PhotoData
import com.geobotanica.geobotanica.util.Measurement
import com.geobotanica.geobotanica.util.MockkUtil.coVerifyOne
import com.geobotanica.geobotanica.util.MockkUtil.mockkBeforeGroup
import com.geobotanica.geobotanica.util.MockkUtil.mockkObserver
import com.geobotanica.geobotanica.util.MockkUtil.mockkObserverNullable
import com.geobotanica.geobotanica.util.MockkUtil.mockkRoomStatic
import com.geobotanica.geobotanica.util.MockkUtil.verifyOne
import com.geobotanica.geobotanica.util.SpekExt.allowLiveData
import com.geobotanica.geobotanica.util.SpekExt.beforeEachBlockingTest
import com.geobotanica.geobotanica.util.SpekExt.mockTime
import com.geobotanica.geobotanica.util.SpekExt.setupTestDispatchers
import com.geobotanica.geobotanica.util.liveData
import io.mockk.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File


object PlantDetailViewModelTest : Spek({
    mockTime()
    allowLiveData()
    val testDispatchers = setupTestDispatchers()

    val plantObserver = mockkObserver<Plant>()
    val plantTypeObserver = mockkObserver<Plant.Type>()
    val createdByUserObserver = mockkObserver<User>()
    val locationObserver = mockkObserver<Location>()
    val photoDataObserver = mockkObserver<List<PhotoData>>()
    val heightObserver = mockkObserverNullable<Measurement>()
    val heightDateObserver = mockkObserver<String>()
    val diameterObserver = mockkObserverNullable<Measurement>()
    val diameterDateTextObserver = mockkObserver<String>()
    val trunkDiameterObserver = mockkObserverNullable<Measurement>()
    val trunkDiameterDateTextObserver = mockkObserver<String>()
    val lastMeasuredByUserObserver = mockkObserver<String>()
    val createdDateTextObserver = mockkObserver<String>()
    val startPhotoIntentObserver = mockkObserver<File>()
    val showPlantDeletedToastObserver = mockkObserver<Unit>()

    val newPhotoFilename = "photo.jpg"
    val newPhotoFile = File(newPhotoFilename)

    val storageHelper = mockk<StorageHelper> {
        every { createPhotoFile() } returns newPhotoFile
        every { getAbsolutePath(any()) } returns newPhotoFilename
        every { deleteFile(any()) } returns true
        every { photoUriFrom(any()) } returns newPhotoFilename
    }

    val fakeUser = User("user").apply { id = 0L }
    val fakePlant = Plant(fakeUser.id, TREE, "common", "scientific", 2L, 3L)
    val fakeLocation = Location(1.0, 2.0, 3.0, 0.1f, 10, 20)
    val fakePlantLocation = PlantLocation(fakePlant.id, fakeLocation)
    val fakePhoto = PlantPhoto(fakeUser.id, fakePlant.id, COMPLETE, "photoUri")
    val fakeHeight = PlantMeasurement(fakeUser.id, fakePlant.id, HEIGHT, 1f)
    val fakeDiameter = PlantMeasurement(fakeUser.id, fakePlant.id, DIAMETER, 2f)
    val fakeTrunkDiameter = PlantMeasurement(fakeUser.id, fakePlant.id, TRUNK_DIAMETER, 3f)


    val userRepo = mockkBeforeGroup<UserRepo> {
        coEvery { get(any<Long>()) } returns fakeUser
        coEvery { getLiveData(any()) } returns liveData(fakeUser)
    }

    val plantRepo = mockkBeforeGroup<PlantRepo> {
        coEvery { getLiveData(any()) } returns liveData(fakePlant)
        coEvery { update(any()) } returns 1
        coEvery { delete(any()) } returns 1
    }
    val plantLocationRepo = mockkBeforeGroup<PlantLocationRepo> {
        coEvery { getLastPlantLocation(any()) } returns liveData(fakePlantLocation)
    }
    val plantPhotoRepo = mockkBeforeGroup<PlantPhotoRepo> {
        coEvery { getAllPhotosOfPlant(any()) } returns listOf(fakePhoto)
        coEvery { getAllPhotosOfPlantLiveData(any()) } returns liveData(listOf(fakePhoto))
        coEvery { update(any()) } returns 1
        coEvery { delete(any()) } returns 1
        coEvery { insert(any()) } returns 0L
    }
    val plantMeasurementRepo = mockkBeforeGroup<PlantMeasurementRepo> {
        coEvery { get(any<Long>()) } returns fakeHeight 
        coEvery { getLastOfPlantLiveData(any(), HEIGHT.flag) } returns liveData(fakeHeight)
        coEvery { getLastOfPlantLiveData(any(), DIAMETER.flag) } returns liveData(fakeDiameter)
        coEvery { getLastOfPlantLiveData(any(), TRUNK_DIAMETER.flag) } returns liveData(fakeTrunkDiameter)
        coEvery { getAllOfPlant(any(), TRUNK_DIAMETER.flag) } returns listOf(fakeTrunkDiameter)
        coEvery { getLastOfPlantLiveData(any()) } returns liveData(fakeHeight)
        coEvery { insert(any()) } returns 0L
        coEvery { delete(any()) } returns 1
    }

    mockkRoomStatic()
    val block = slot<suspend () -> Unit>()
    val database = mockk<GbDatabase> {
        coEvery {
            withTransaction(capture(block))
        } coAnswers  {
            block.captured.invoke()
        }
    }

    val plantDetailViewModel by memoized {
        PlantDetailViewModel(
                testDispatchers,
                database,
                storageHelper,
                userRepo,
                plantRepo,
                plantLocationRepo,
                plantPhotoRepo,
                plantMeasurementRepo
        ).apply {
            plant.observeForever(plantObserver)
            plantType.observeForever(plantTypeObserver)
            createdByUser.observeForever(createdByUserObserver)
            location.observeForever(locationObserver)
            photoData.observeForever(photoDataObserver)
            height.observeForever(heightObserver)
            heightDateText.observeForever(heightDateObserver)
            diameter.observeForever(diameterObserver)
            diameterDateText.observeForever(diameterDateTextObserver)
            trunkDiameter.observeForever(trunkDiameterObserver)
            trunkDiameterDateText.observeForever(trunkDiameterDateTextObserver)
            lastMeasuredByUser.observeForever(lastMeasuredByUserObserver)
            createdDateText.observeForever(createdDateTextObserver)
            startPhotoIntent.observeForever(startPhotoIntentObserver)
            showPlantDeletedToast.observeForever(showPlantDeletedToastObserver)
        }
    }


    beforeEachTest {
        clearMocks(
                plantObserver,
                plantTypeObserver,
                createdByUserObserver,
                locationObserver,
                photoDataObserver,
                heightObserver,
                heightDateObserver,
                diameterObserver,
                diameterDateTextObserver,
                trunkDiameterObserver,
                trunkDiameterDateTextObserver,
                lastMeasuredByUserObserver,
                createdDateTextObserver,
                startPhotoIntentObserver,
                showPlantDeletedToastObserver,

                storageHelper,
                database,
                userRepo,
                plantRepo,
                plantLocationRepo,
                plantPhotoRepo,
                plantMeasurementRepo,
                answers = false // Keep stubbing, reset recorded calls
        )
    }



    describe("Delete photo") {
        beforeEachBlockingTest(testDispatchers) { plantDetailViewModel.onDeletePhoto(0) }

        it("Should delete plant") { coVerifyOne { plantPhotoRepo.delete(fakePhoto) } }
        it("Should show plant deleted toast") { verifyOne { showPlantDeletedToastObserver.onChanged(null) } }
    }

    describe("Retake photo") {
        beforeEachTest { plantDetailViewModel.onRetakePhoto() }

        it("Should start photo capture") { verifyOne { startPhotoIntentObserver.onChanged(newPhotoFile) } }

        context("Photo captured") {
            beforeEachTest { plantDetailViewModel.onPhotoComplete(0) }

            it("Should update photo") {
                coVerifyOne { plantPhotoRepo.update(fakePhoto.copy(filename = newPhotoFilename)) }
            }
        }
    }

    describe("Add photo") {
        beforeEachTest { plantDetailViewModel.onAddPhoto(COMPLETE) }

        it("Should start photo capture") { verifyOne { startPhotoIntentObserver.onChanged(newPhotoFile) } }

        context("Cancel photo capture") {
            beforeEachTest { plantDetailViewModel.deleteTemporaryPhoto() }

            it("Should delete temp photo") { verifyOne { storageHelper.deleteFile(newPhotoFilename) } }
        }

        context("Photo captured") {
            val plantPhoto by memoized { PlantPhoto(fakeUser.id, fakePlant.id, COMPLETE, newPhotoFilename) }

            beforeEachTest { plantDetailViewModel.onPhotoComplete(0) }

            it("Should insert photo in db") { coVerifyOne { plantPhotoRepo.insert(plantPhoto) } }
        }
    }

    describe("New plant type") {
        beforeEachTest { plantDetailViewModel.onNewPlantType(SHRUB) }

        it("Should update plant type") {
            coVerifyOne { plantRepo.update(fakePlant.copy(type = SHRUB)) }
        }
        it("Should delete trunk diameters") { coVerifyOne { plantMeasurementRepo.delete(fakeTrunkDiameter) } }
    }

    describe("Update plant names") {
        beforeEachTest {
            plantDetailViewModel.onUpdatePlantNames("common2", "scientific2")
        }

        it("Should update plant names") {
            coVerifyOne {
                plantRepo.update(fakePlant.copy(
                        commonName = "common2",
                        scientificName = "scientific2"
                ))
            }
        }
    }

    describe("Update photo type") {
        beforeEachTest { plantDetailViewModel.onUpdatePhotoType(0, FLOWER) }

        it("Should update photo type") {
            coVerifyOne { plantPhotoRepo.update(fakePhoto.copy(type = FLOWER)) }
        }
    }

    describe("New measurements") {
        val newHeight by memoized { Measurement(4f) }
        val newDiameter by memoized { Measurement(5f) }
        val newTrunkDiameter by memoized { Measurement(6f) }
        val newPlantHeight by memoized { PlantMeasurement(fakeUser.id, fakePlant.id, HEIGHT, 4f) }
        val newPlantDiameter by memoized { PlantMeasurement(fakeUser.id, fakePlant.id, DIAMETER, 5f) }
        val newPlantTrunkDiameter by memoized { PlantMeasurement(fakeUser.id, fakePlant.id, TRUNK_DIAMETER, 6f) }

        beforeEachTest {
            plantDetailViewModel.onMeasurementsAdded(newHeight, newDiameter, newTrunkDiameter)
        }

        it("Should insert measurements in db") {
            coVerifyOrder {
                plantMeasurementRepo.insert(newPlantHeight)
                plantMeasurementRepo.insert(newPlantDiameter)
                plantMeasurementRepo.insert(newPlantTrunkDiameter)
            }
        }
    }

    describe("Delete plant") {
        beforeEachTest {
            plantDetailViewModel.markPlantForDeletion()
            plantDetailViewModel.onDestroyFragment()
        }

        it("Should delete plant") { coVerifyOne { plantRepo.delete(fakePlant) } }
    }
})