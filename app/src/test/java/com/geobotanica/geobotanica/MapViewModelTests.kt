package com.geobotanica.geobotanica

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.ui.map.MapViewModel
import com.geobotanica.geobotanica.ui.map.PlantMarkerData
import com.geobotanica.geobotanica.util.SpekLiveData.describeWithLiveData
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.threeten.bp.OffsetDateTime

class MapViewModelTests : Spek({

    describeWithLiveData("MapViewModel") {
        val plantCompositesLiveData = MutableLiveData<List<PlantComposite>>()

        val storageHelper = mockk<StorageHelper>()
        val mapRepo = mockk<MapRepo>()
        val assetRepo = mockk<AssetRepo>()

        val plantRepo = mockk<PlantRepo>()
        every { plantRepo.getAllPlantComposites() } returns plantCompositesLiveData

        val locationService = mockk<LocationService>()
        every { locationService.unsubscribe(any()) } returns Unit
        every { locationService.subscribe(any(), any(), any()) } returns Unit

        val mapViewModel = MapViewModel(storageHelper, mapRepo, assetRepo, plantRepo, locationService)

        val showSnackbarObserver = mockk<Observer<Unit>>(relaxed = true)
        mapViewModel.showGpsRequiredSnackbar.observeForever(showSnackbarObserver)

        context("GPS init subscribe while GPS disabled") {
            every { locationService.isGpsEnabled() } returns false
            mapViewModel.initGpsSubscribe()

            it("Should trigger GPS required snackbar") {
                verify { showSnackbarObserver.onChanged(any()) }
            }
        }
        context("GPS init subscribe while GPS enabled") {
            every { locationService.isGpsSubscribed(any()) } returns false
            every { locationService.isGpsEnabled() } returns true
            mapViewModel.initGpsSubscribe()

            it("Should subscribe GPS") {
                verify { locationService.subscribe(mapViewModel, any(), any()) }
            }
        }

        context("onClickGpsFab() while GPS disabled") {
            every { locationService.isGpsEnabled() } returns false

            mapViewModel.onClickGpsFab()

            it("Should trigger GPS required snackbar") {
                verify { showSnackbarObserver.onChanged(any()) }
            }

        }
        context("onClickGpsFab() while GPS enabled") {
            every { locationService.isGpsEnabled() } returns true

            context("If GPS already subscribed") {
                every { locationService.isGpsSubscribed(any()) } returns true
                mapViewModel.onClickGpsFab()

                it("Should unsubscribe GPS") {
                    verify { locationService.unsubscribe(mapViewModel) }
                }
            }
            context("If GPS not subscribed") {
                every { locationService.isGpsSubscribed(any()) } returns false
                mapViewModel.onClickGpsFab()

                it("Should subscribe GPS") {
                    verify { locationService.subscribe(mapViewModel, any(), any()) }
                }
            }
        }

        describeWithLiveData("PlantComposites LiveData") {
            val plantComposite = PlantComposite()
            val plant = Plant(1L, Plant.Type.TREE, "common", "latin", 1L, 1L, OffsetDateTime.MAX)
            plant.id = 1L
            plantComposite.plant = plant
            plantComposite.plantLocations = listOf(
                    PlantLocation(1L, Location(1.0, 2.0, 3.0,1.0f,
                            10, 20, OffsetDateTime.MAX))
            )
            plantComposite.plantPhotos = listOf(
                    PlantPhoto(1L, 1L, PlantPhoto.Type.COMPLETE,"photoPath", OffsetDateTime.MAX)
            )
            plantComposite.plantMeasurements = listOf(
                    PlantMeasurement(1L, 1L, PlantMeasurement.Type.HEIGHT,5.0f, OffsetDateTime.MAX),
                    PlantMeasurement(1L, 1L, PlantMeasurement.Type.DIAMETER,0.5f, OffsetDateTime.MAX)
            )

            plantCompositesLiveData.value = listOf(plantComposite)

            context("Capture LiveData") {
                val plantMarkerDataObserver = mockk<Observer< List<PlantMarkerData>> >()
                val slot = slot< List<PlantMarkerData> >()
                every { plantMarkerDataObserver.onChanged(capture(slot)) } returns Unit

                mapViewModel.plantMarkerData.observeForever(plantMarkerDataObserver)

                it("Should extract to PlantMarkerData list") {
                    slot.captured shouldEqual listOf(PlantMarkerData(
                        1L, Plant.Type.TREE,"common", "latin",
                        1.0, 2.0, "photoPath",
                        OffsetDateTime.MAX.toString().substringBefore('T')
                    ))
                }
            }
        }
    }
})