package com.geobotanica.geobotanica

import androidx.lifecycle.Observer
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.ui.map.MapViewModel
import com.geobotanica.geobotanica.util.SpekLiveData.describeWithLiveData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.spekframework.spek2.Spek

class MapViewModelTests : Spek({

    val showSnackbarObserver by memoized { mockk<Observer<Unit>>(relaxed = true) }
    val locationService by memoized { mockk<LocationService>(relaxed = true) }
    val mapViewModel by memoized {
        MapViewModel(mockk(), mockk(), mockk(), mockk(), locationService).apply {
            showGpsRequiredSnackbar.observeForever(showSnackbarObserver)
        }
    }

    describeWithLiveData("GPS Initialization") {

        context("When GPS enabled") {
            beforeEachTest { every { locationService.isGpsEnabled() } returns true }

            context("When not subscribed to GPS") {
                beforeEachTest { every { locationService.isGpsSubscribed(any()) } returns false }

                it("Should subscribe to GPS") {
                    mapViewModel.initGpsSubscribe()
                    verify { locationService.subscribe(mapViewModel, any(), any()) }
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
                verify { showSnackbarObserver.onChanged(any()) }
            }
        }
    }

    describeWithLiveData("GPS FAB Click") {

        context("When GPS disabled") {
            beforeEachTest { every { locationService.isGpsEnabled() } returns false }

            it("Should trigger GPS required snackbar") {
                mapViewModel.onClickGpsFab()
                verify { showSnackbarObserver.onChanged(any()) }
            }
        }

        context("When GPS enabled") {
            beforeEachTest { every { locationService.isGpsEnabled() } returns true }

            context("When GPS already subscribed") {
                beforeEachTest { every { locationService.isGpsSubscribed(any()) } returns true }

                it("Should unsubscribe GPS") {
                    mapViewModel.onClickGpsFab()
                    verify { locationService.unsubscribe(mapViewModel) }
                }
            }
            context("When GPS not subscribed") {
                beforeEachTest { every { locationService.isGpsSubscribed(any()) } returns false }

                it("Should subscribe GPS") {
                    mapViewModel.onClickGpsFab()
                    verify { locationService.subscribe(mapViewModel, any(), any()) }
                }
            }
        }
    }
})