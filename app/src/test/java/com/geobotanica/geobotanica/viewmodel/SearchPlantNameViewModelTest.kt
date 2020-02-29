package com.geobotanica.geobotanica.viewmodel

import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.ui.searchplantname.SearchPlantNameViewModel
import com.geobotanica.geobotanica.ui.searchplantname.ViewEffect
import com.geobotanica.geobotanica.ui.searchplantname.ViewEffect.*
import com.geobotanica.geobotanica.ui.searchplantname.ViewEvent.*
import com.geobotanica.geobotanica.ui.searchplantname.ViewState
import com.geobotanica.geobotanica.util.MockkUtil.mockkObserver
import com.geobotanica.geobotanica.util.MockkUtil.verifyOne
import com.geobotanica.geobotanica.util.SpekExt.allowLiveData
import com.geobotanica.geobotanica.util.SpekExt.beforeEachBlockingTest
import com.geobotanica.geobotanica.util.SpekExt.setupTestDispatchers
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SearchPlantNameViewModelTest : Spek({
    allowLiveData()
    val testDispatchers = setupTestDispatchers()

    val viewStateObserver = mockkObserver<ViewState>()
    val viewEffectObserver = mockkObserver<ViewEffect>()

    val taxonRepo = mockk<TaxonRepo> {
        coEvery { setTagged(any(), any()) } returns Unit
        coEvery { updateTagTimestamp(any(), any()) } returns Unit
    }
    val vernacularRepo = mockk<VernacularRepo> {
        coEvery { setTagged(any(), any()) } returns Unit
        coEvery { updateTagTimestamp(any(), any()) } returns Unit
    }
    val plantNameSearchService = mockk<PlantNameSearchService>()

    val searchPlantNameViewModel by memoized {
        SearchPlantNameViewModel(testDispatchers, taxonRepo, vernacularRepo, plantNameSearchService).apply {
            viewState.observeForever(viewStateObserver)
            viewEffect.observeForever(viewEffectObserver)
        }
    }

    val userId = 0L
    val photoUri = "photoUri"

    beforeEachTest {
        clearMocks(
                viewStateObserver,
                viewEffectObserver,
                taxonRepo,
                vernacularRepo,
                plantNameSearchService,
                answers = false // Keep stubbing, reset recorded calls
        )
    }

    describe("ViewCreated Event") {
        beforeEachTest { searchPlantNameViewModel.onEvent(ViewCreated(userId, photoUri)) }

        it("Should emit InitView effect once") {
            verifyOne { viewEffectObserver.onChanged(InitView) }
        }

        it("Should emit default ViewState") {
            verify { viewStateObserver.onChanged(ViewState()) }
        }
    }

    describe("OnStart Event") {

        val searchResults = listOf(
                        SearchResult(1L, 2, 4, "name"),
                        SearchResult(8L, 16, 32, "name"))

        beforeEachBlockingTest(testDispatchers) {
            every { plantNameSearchService.search("string") } returns flowOf(searchResults)
            searchPlantNameViewModel.onEvent(OnStart("string"))
        }

        it("Should emit correct ViewStates/ViewEffects") {
            verifyOrder {
                viewStateObserver.onChanged(ViewState())
                viewStateObserver.onChanged(ViewState(searchEditText = "string", isLoadingSpinnerVisible = true))
                viewEffectObserver.onChanged(UpdateSearchResults(searchResults))
                viewStateObserver.onChanged(
                        ViewState(searchEditText = "string", isLoadingSpinnerVisible = false, searchResults = searchResults)
                )
            }
        }
    }

    describe("SearchFilterSelected Event") {
        val searchFilterOptions = PlantNameSearchService.SearchFilterOptions.fromBooleans(
                isCommonFiltered = true,
                isScientificFiltered = false,
                isStarredFiltered = false,
                isUsedFiltered = false)
        val searchResults: List<SearchResult> = emptyList()

        beforeEachBlockingTest(testDispatchers) {
            every { plantNameSearchService.search(any(), any()) } returns flowOf(searchResults)
            searchPlantNameViewModel.onEvent(SearchFilterSelected(searchFilterOptions))
        }

        it("Should emit correct ViewStates/ViewEffects") {
            verifyOrder {
                viewEffectObserver.onChanged(UpdateSharedPrefs(searchFilterOptions))
                viewStateObserver.onChanged(ViewState(searchFilterOptions = searchFilterOptions))
                viewStateObserver.onChanged(ViewState(searchFilterOptions = searchFilterOptions, isLoadingSpinnerVisible = true))
                viewEffectObserver.onChanged(UpdateSearchResults(searchResults))
                viewStateObserver.onChanged(ViewState(searchFilterOptions = searchFilterOptions, isLoadingSpinnerVisible = false))
            }
        }
    }

    describe("ResultClicked Event") {
        val searchResult by memoized { SearchResult(1L, 0, 0, "name") }

        beforeEachTest { searchPlantNameViewModel.onEvent(ViewCreated(userId, photoUri)) }

        context("When result has SCIENTIFIC tag") {
            beforeEachTest {
                searchResult.toggleTag(SCIENTIFIC)
                searchPlantNameViewModel.onEvent(ResultClicked(searchResult))
            }

            it("Should emit NavigateToNext ViewEffect") {
                verifyOne { viewEffectObserver.onChanged(NavigateToNext(userId, photoUri, 1L, null)) }
            }
        }

        context("When result has COMMON tag") {
            beforeEachTest {
                searchResult.toggleTag(COMMON)
                searchPlantNameViewModel.onEvent(ResultClicked(searchResult))
            }

            it("Should emit NavigateToNext ViewEffect") {
                verifyOne { viewEffectObserver.onChanged(NavigateToNext(userId, photoUri, null, 1L)) }
            }
        }

        context("When result has STARRED tag") {
            beforeEachTest { searchResult.toggleTag(STARRED) }

            context("When result has COMMON tag") {
                beforeEachTest {
                    searchResult.toggleTag(COMMON)
                    searchPlantNameViewModel.onEvent(ResultClicked(searchResult))
                }

                it("Should update vernacular repo timestamp") {
                    coVerify { vernacularRepo.updateTagTimestamp(any(), STARRED) }
                }
            }

            context("When result has SCIENTIFIC tag") {
                beforeEachTest {
                    searchResult.toggleTag(SCIENTIFIC)
                    searchPlantNameViewModel.onEvent(ResultClicked(searchResult))
                }

                it("Should update taxon repo timestamp") {
                    coVerify { taxonRepo.updateTagTimestamp(any(), STARRED) }
                }
            }
        }
    }

    describe("StarClicked Event") {
        val searchResult by memoized {
            SearchResult(1L, 0, 0, "name").apply { toggleTag(STARRED) }
        }

        context("When result has COMMON tag") {
            beforeEachTest {
                searchResult.toggleTag(COMMON)
                searchPlantNameViewModel.onEvent(StarClicked(searchResult))
            }

            it("Should set starred tag in vernacular repo") {
                coVerify { vernacularRepo.setTagged(1L, STARRED) }
            }
        }

        context("When result has SCIENTIFIC tag") {
            beforeEachTest {
                searchResult.toggleTag(SCIENTIFIC)
                searchPlantNameViewModel.onEvent(StarClicked(searchResult))
            }

            it("Should set starred tag in taxon repo") {
                coVerify { taxonRepo.setTagged(1L, STARRED) }
            }
        }
    }

    describe("SearchEditTextChanged Event") {

        beforeEachBlockingTest(testDispatchers) {
            searchPlantNameViewModel.onEvent(SearchEditTextChanged("string"))
        }

        it("Should emit correct ViewStates/ViewEffects") {
            verifyOrder {
                viewStateObserver.onChanged(ViewState(searchEditText = "string"))
                viewStateObserver.onChanged(ViewState(searchEditText = "string", isLoadingSpinnerVisible = true))
                viewEffectObserver.onChanged(UpdateSearchResults(emptyList()))
                viewStateObserver.onChanged(ViewState(searchEditText = "string", isLoadingSpinnerVisible = false))
            }
        }

        context("When search text is same as previous") {
            beforeEachTest {
                searchPlantNameViewModel.onEvent(SearchEditTextChanged("string"))
                searchPlantNameViewModel.onEvent(SearchEditTextChanged("string"))
            }

            it("Should perform search only once") {
                verifyOne { plantNameSearchService.search("string") }
            }
        }
    }

    describe("ClearSearchClicked Event") {
        beforeEachTest { searchPlantNameViewModel.onEvent(ClearSearchClicked) }

        it("Should emit ClearSearchText ViewEffect") {
            verifyOne { viewEffectObserver.onChanged(ClearSearchText) }
        }
    }

    describe("SkipClicked Event") {
        beforeEachTest {
            searchPlantNameViewModel.onEvent(ViewCreated(userId, photoUri))
            searchPlantNameViewModel.onEvent(SkipClicked)
        }

        it("Should emit NavigateToNext ViewEffect") {
            verifyOne { viewEffectObserver.onChanged(NavigateToNext(userId, photoUri, null, null)) }
        }
    }
})