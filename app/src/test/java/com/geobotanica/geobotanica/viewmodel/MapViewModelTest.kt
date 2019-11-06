package com.geobotanica.geobotanica.viewmodel

import androidx.lifecycle.Observer
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.ui.map.MapViewModel
import com.geobotanica.geobotanica.util.SpekLiveData.allowLiveData
import io.mockk.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class MapViewModelTest : Spek({
    allowLiveData()

    val showSnackbarObserver = mockk<Observer<Unit>>(relaxed = true)
    val locationService = mockk<LocationService>(relaxed = true)
    val mapViewModel = MapViewModel(mockk(), mockk(), mockk(), mockk(), mockk(), locationService).apply {
        showGpsRequiredSnackbar.observeForever(showSnackbarObserver)
    }

    beforeEachTest { clearMocks(showSnackbarObserver, locationService, answers = false) } // Keep stubbing, reset recorded calls

    describe("GPS Initialization") {

        context("When GPS enabled") {
            beforeGroup { every { locationService.isGpsEnabled() } returns true }

            context("When not subscribed to GPS") {
                beforeEachTest { every { locationService.isGpsSubscribed(mapViewModel) } returns false }

                it("Should subscribe to GPS") {
                    mapViewModel.initGpsSubscribe()
                    verify(exactly = 1) { locationService.subscribe(mapViewModel, any(), any()) }
                }
            }

            context("When already subscribed to GPS") {
                beforeEachTest { every { locationService.isGpsSubscribed(mapViewModel) } returns true }

                it("Should not subscribe to GPS") {
                    mapViewModel.initGpsSubscribe()
                    verify(exactly = 0) { locationService.subscribe(mapViewModel, any(), any()) }
                }
            }
        }

        context("When GPS disabled") {
            beforeEachTest { every { locationService.isGpsEnabled() } returns false }

            it("Should trigger GPS required snackbar") {
                mapViewModel.initGpsSubscribe()
                verify(exactly = 1) { showSnackbarObserver.onChanged(any()) }
            }
        }
    }

    describe("GPS FAB Click") {

        context("When GPS disabled") {
            beforeEachTest { every { locationService.isGpsEnabled() } returns false }

            it("Should trigger GPS required snackbar") {
                mapViewModel.onClickGpsFab()
                verify(exactly = 1) { showSnackbarObserver.onChanged(any()) }
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
                    verify(exactly = 1) { locationService.subscribe(mapViewModel, any(), any()) }
                }
            }
        }
    }
})