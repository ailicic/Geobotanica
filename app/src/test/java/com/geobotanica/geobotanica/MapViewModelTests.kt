package com.geobotanica.geobotanica

import com.geobotanica.geobotanica.util.SpekLiveData.describeWithLiveData
import androidx.lifecycle.Observer
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.ui.map.MapViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.spekframework.spek2.Spek


class MapViewModelTests : Spek({

    describeWithLiveData("MapViewModel") {
//        val plantCompositeList = mutableListOf<PlantComposite>()
//        plantCompositeList.add(PlantComposite(
//                Plant()
//        ))

//        val plantCompositesLiveData = MutableLiveData<List<PlantComposite>>()
//        plantCompositesLiveData.value = plantCompositeList

        val plantRepo = mockk<PlantRepo>()
//        every { plantRepo.getAllPlantComposites() } returns plantCompositesLiveData

        val locationService = mockk<LocationService>()
        every { locationService.unsubscribe(any()) } returns Unit
        every { locationService.subscribe(any(), any(), any()) } returns Unit

        val mapViewModel = MapViewModel(plantRepo, locationService)

        context("onClickGpsFab() while GPS disabled") {
            every { locationService.isGpsEnabled() } returns false

            val showSnackbarObserver = mockk<Observer<Unit>>(relaxed = true)

            mapViewModel.showGpsRequiredSnackbar.observeForever(showSnackbarObserver)
            mapViewModel.onClickGpsFab()

            it("Should trigger GPS required snackbar") {
                verify { showSnackbarObserver.onChanged(any())}
            }

        }
        context("GPS enabled") {
            every { locationService.isGpsEnabled() } returns true

            context("onClickGpsFab() while GPS subscribed") {
                every { locationService.isGpsSubscribed(any()) } returns true
                mapViewModel.onClickGpsFab()

                it("Should unsubscribe GPS") {
                    verify {
                        locationService.unsubscribe(mapViewModel)
                    }
                }
            }

            context("onClickGpsFab() while GPS unsubscribed") {
                every { locationService.isGpsSubscribed(any()) } returns false
                mapViewModel.onClickGpsFab()

                it("Should subscribe GPS") {
                    verify {
                        locationService.subscribe(mapViewModel, any(), any())
                    }
                }
            }
        }
    }
})