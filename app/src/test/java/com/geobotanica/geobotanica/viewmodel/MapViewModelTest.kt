package com.geobotanica.geobotanica.viewmodel

import androidx.lifecycle.Observer
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.location.Location
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.data.entity.OnlineAssetId
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DOWNLOADED
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.NOT_DOWNLOADED
import com.geobotanica.geobotanica.ui.map.MapViewModel
import com.geobotanica.geobotanica.ui.map.MapViewModel.GpsFabDrawable.GPS_FIX
import com.geobotanica.geobotanica.ui.map.MapViewModel.GpsFabDrawable.GPS_OFF
import com.geobotanica.geobotanica.ui.map.marker.PlanterMarkerDiffer
import com.geobotanica.geobotanica.util.SpekExt.allowLiveData
import com.geobotanica.geobotanica.util.SpekExt.beforeEachBlockingTest
import com.geobotanica.geobotanica.util.SpekExt.setupTestDispatchers
import io.mockk.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object MapViewModelTest : Spek({
    allowLiveData()
    setupTestDispatchers()

    val gpsRequiredSnackbarObserver = mockk<Observer<Unit>>(relaxed = true)
    val plantNamesMissingSnackbarObserver = mockk<Observer<Unit>>(relaxed = true)
    val gpsFabIconObserver = mockk<Observer<Int>>(relaxed = true)
    val locationPrecisionMarkerObserver = mockk<Observer<Location>>(relaxed = true)
    val locationMarkerObserver = mockk<Observer<Location>>(relaxed = true)
    val centerMapObserver = mockk<Observer<Location>>(relaxed = true)
    val redrawMapLayersObserver = mockk<Observer<Unit>>(relaxed = true)
    val navigateToNewPlantObserver = mockk<Observer<Unit>>(relaxed = true)

    val storageHelper = mockk<StorageHelper>()
    val mapRepo = mockk<MapRepo>()
    val assetRepo = mockk<AssetRepo>()
    val plantRepo = mockk<PlantRepo>()
    val plantMarkerDiffer = mockk<PlanterMarkerDiffer>()
    val locationService = mockk<LocationService>(relaxed = true)

    val mapViewModel by memoized {
        MapViewModel(storageHelper, mapRepo, assetRepo, plantRepo, plantMarkerDiffer, locationService).apply {
            wasGpsSubscribed = true
            showGpsRequiredSnackbar.observeForever(gpsRequiredSnackbarObserver)
            showPlantNamesMissingSnackbar.observeForever(plantNamesMissingSnackbarObserver)
            gpsFabIcon.observeForever(gpsFabIconObserver)
            updateLocationPrecisionMarker.observeForever(locationPrecisionMarkerObserver)
            updateLocationMarker.observeForever(locationMarkerObserver)
            centerMap.observeForever(centerMapObserver)
            redrawMapLayers.observeForever(redrawMapLayersObserver)
            navigateToNewPlant.observeForever(navigateToNewPlantObserver)
        }
    }


    beforeEachTest {
        clearMocks(
                assetRepo,
                locationService,
                gpsRequiredSnackbarObserver,
                plantNamesMissingSnackbarObserver,
                gpsFabIconObserver,
                locationPrecisionMarkerObserver,
                locationMarkerObserver,
                centerMapObserver,
                redrawMapLayersObserver,
                navigateToNewPlantObserver,
                answers = false // Keep stubbing, reset recorded calls
        )
    }

    describe("GPS Initialization") {

        context("When GPS enabled") {
            beforeGroup { every { locationService.isGpsEnabled() } returns true }

            context("When not subscribed to GPS") {
                beforeEachTest { every { locationService.isGpsSubscribed(mapViewModel) } returns false }

                it("Should subscribe to GPS") {
                    mapViewModel.initGpsSubscribe()
                    verify(exactly = 1) { locationService.subscribe(mapViewModel, any()) }
                }
            }

            context("When already subscribed to GPS") {
                beforeEachTest { every { locationService.isGpsSubscribed(mapViewModel) } returns true }

                it("Should not subscribe to GPS") {
                    mapViewModel.initGpsSubscribe()
                    verify(exactly = 0) { locationService.subscribe(mapViewModel, any()) }
                }
            }
        }

        context("When GPS disabled") {
            beforeEachTest { every { locationService.isGpsEnabled() } returns false }

            it("Should trigger GPS required snackbar") {
                mapViewModel.initGpsSubscribe()
                verify(exactly = 1) { gpsRequiredSnackbarObserver.onChanged(any()) }
            }
        }
    }

    describe("GPS FAB Click") {

        context("When GPS disabled") {
            beforeEachTest { every { locationService.isGpsEnabled() } returns false }

            it("Should trigger GPS required snackbar") {
                mapViewModel.onClickGpsFab()
                verify(exactly = 1) { gpsRequiredSnackbarObserver.onChanged(any()) }
            }
        }

        context("When GPS enabled") {
            beforeEachTest { every { locationService.isGpsEnabled() } returns true }

            context("When GPS already subscribed") {
                beforeEachTest { every { locationService.isGpsSubscribed(mapViewModel) } returns true }

                it("Should unsubscribe GPS") {
                    mapViewModel.onClickGpsFab()
                    verify(exactly = 1) { locationService.unsubscribe(mapViewModel) }
                }
            }

            context("When GPS not subscribed") {
                beforeEachTest { every { locationService.isGpsSubscribed(mapViewModel) } returns false }

                it("Should subscribe GPS") {
                    mapViewModel.onClickGpsFab()
                    verify(exactly = 1) { locationService.subscribe(mapViewModel, any()) }
                }
            }
        }
    }

    describe("Location received") {
        val emptyLocation = Location(null, null, satellitesVisible = 0)

        val staleLocation = mockk<Location> {
            every { latitude } returns 1.0
            every { longitude } returns 1.0
            every { isRecent() } returns false
        }

        val recentLocation = mockk<Location> {
            every { latitude } returns 1.0
            every { longitude } returns 1.0
            every { isRecent() } returns true
        }

        context("Without lat/long"){
            beforeEachTest { mapViewModel.onLocation(emptyLocation) }

            it("Should ignore") {
                verify(exactly = 0) { gpsFabIconObserver.onChanged(any()) }
                verify(exactly = 0) { locationPrecisionMarkerObserver.onChanged(any()) }
                verify(exactly = 0) { locationMarkerObserver.onChanged(any()) }
                verify(exactly = 0) { centerMapObserver.onChanged(any()) }
                verify(exactly = 0) { redrawMapLayersObserver.onChanged(any()) }
            }

        }

        context("With cached timestamp"){
            beforeEachTest { mapViewModel.onLocation(staleLocation) }

            it("Should update location marker") {
                verifySequence {
                    locationMarkerObserver.onChanged(any())
                    redrawMapLayersObserver.onChanged(any())
                }
            }
        }


        context("With recent timestamp"){
            beforeEachTest { mapViewModel.onLocation(recentLocation) }

            it("Should set GpsFabIcon to GPS_FIX") {
                verify(exactly = 1) { gpsFabIconObserver.onChanged(GPS_FIX.drawable) }
            }

            it("Should update location precision marker") {
                verify(exactly = 1) { locationPrecisionMarkerObserver.onChanged(any()) }
            }

            it("Should center map") {
                verify(exactly = 1) { centerMapObserver.onChanged(any()) }
            }

            it("Should update location marker") {
                verify(exactly = 1) { locationMarkerObserver.onChanged(any()) }
            }

            it("Should redraw map") {
                verify(exactly = 1) { redrawMapLayersObserver.onChanged(any()) }
            }
        }
    }

    describe("New Plant FAB Click") {

        context("When GPS disabled") {
            beforeEachBlockingTest {
                every { locationService.isGpsEnabled() } returns false
                mapViewModel.onClickNewPlantFab()
            }

            it("Should show GPS required snackbar") {
                verify(exactly = 1) { gpsRequiredSnackbarObserver.onChanged(any()) }
            }

            it("Should set GPS FAB icon to GPS_OFF") {
                verify(exactly = 1) { gpsFabIconObserver.onChanged(GPS_OFF.drawable) }
            }
        }

        context("When GPS enabled") {
            beforeEachTest { every { locationService.isGpsEnabled() } returns true }

            context("When plant names not available") {
                beforeEachBlockingTest {
                    val missingAsset = OnlineAsset("", "", "", false, 0L, 0L, NOT_DOWNLOADED)
                    coEvery { assetRepo.get(OnlineAssetId.PLANT_NAMES.id) } returns missingAsset
                    mapViewModel.onClickNewPlantFab()
                }

                it("Should show 'Plant names missing' snackbar") {
                    verify(exactly = 1) { plantNamesMissingSnackbarObserver.onChanged(any()) }
                }
            }

            context("When plant names available") {
                beforeEachBlockingTest {
                    val downloadedAsset = OnlineAsset("", "", "", false, 0L, 0L, DOWNLOADED)
                    coEvery { assetRepo.get(OnlineAssetId.PLANT_NAMES.id) } returns downloadedAsset
                    mapViewModel.onClickNewPlantFab()
                }

                it("Should navigate to new plant screen") {
                    verify(exactly = 1) { navigateToNewPlantObserver.onChanged(any()) }
                }
            }
        }
    }



    describe("GPS Unsubscribe") {

        context("When GPS subscribed") {
            beforeEachTest {
                every { locationService.isGpsSubscribed(any()) } returns true
                mapViewModel.unsubscribeGps()
            }

            it("Should set GPS FAB icon to GPS_OFF") {
                verify(exactly = 1) { gpsFabIconObserver.onChanged(GPS_OFF.drawable) }
            }

            it("Should unsubscribe GPS") {
                verify(exactly = 1) { locationService.unsubscribe(any()) }
            }
        }

        context("When GPS not subscribed") {
            beforeEachTest {
                every { locationService.isGpsSubscribed(any()) } returns false
                mapViewModel.unsubscribeGps()
            }

            it("Should do nothing") {
                verify(exactly = 0) { gpsFabIconObserver.onChanged(GPS_OFF.drawable) }
                verify(exactly = 0) { locationService.unsubscribe(mapViewModel) }
            }
        }
    }

})