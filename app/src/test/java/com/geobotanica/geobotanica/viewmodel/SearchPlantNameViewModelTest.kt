package com.geobotanica.geobotanica.viewmodel

import androidx.lifecycle.Observer
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.ui.searchplantname.SearchPlantNameViewModel
import com.geobotanica.geobotanica.ui.searchplantname.ViewAction
import com.geobotanica.geobotanica.ui.searchplantname.ViewAction.*
import com.geobotanica.geobotanica.ui.searchplantname.ViewEvent.*
import com.geobotanica.geobotanica.ui.searchplantname.ViewState
import com.geobotanica.geobotanica.util.SpekExt.allowLiveData
import com.geobotanica.geobotanica.util.SpekExt.beforeEachBlockingTest
import com.geobotanica.geobotanica.util.SpekExt.setupTestDispatchers
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SearchPlantNameViewModelTest : Spek({
    allowLiveData()
    val testDispatchers = setupTestDispatchers()

    val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)
    val viewActionObserver = mockk<Observer<ViewAction>>(relaxed = true)

    val taxonRepo = mockk<TaxonRepo>(relaxed = true)
    val vernacularRepo = mockk<VernacularRepo>(relaxed = true)
    val plantNameSearchService = mockk<PlantNameSearchService>()

    val searchPlantNameViewModel by memoized {
        SearchPlantNameViewModel(testDispatchers, taxonRepo, vernacularRepo, plantNameSearchService).apply {
            viewState.observeForever(viewStateObserver)
            viewAction.observeForever(viewActionObserver)
        }
    }

    beforeEachTest {
        clearMocks(
                viewStateObserver,
                viewActionObserver,
                taxonRepo,
                vernacularRepo,
                plantNameSearchService,
                answers = false // Keep stubbing, reset recorded calls
        )
    }

    describe("ViewCreated Event") {
        beforeEachTest { searchPlantNameViewModel.onEvent(ViewCreated) }

        it("Should emit InitView action once") {
            verify(exactly = 1) { viewActionObserver.onChanged(InitView) }
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

        it("Should emit correct ViewStates/ViewActions") {
            verifyOrder {
                viewStateObserver.onChanged(ViewState(searchEditText = "string"))
                viewStateObserver.onChanged(ViewState(searchEditText = "string", isLoadingSpinnerVisible = true))
                viewActionObserver.onChanged(UpdateSearchResults(searchResults))
                viewStateObserver.onChanged(ViewState(searchEditText = "string", isLoadingSpinnerVisible = false))
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

        it("Should emit correct ViewStates/ViewActions") {
            verifyOrder {
                viewActionObserver.onChanged(UpdateSharedPrefs(searchFilterOptions))
                viewStateObserver.onChanged(ViewState(searchFilterOptions = searchFilterOptions))
                viewStateObserver.onChanged(ViewState(searchFilterOptions = searchFilterOptions, isLoadingSpinnerVisible = true))
                viewActionObserver.onChanged(UpdateSearchResults(searchResults))
                viewStateObserver.onChanged(ViewState(searchFilterOptions = searchFilterOptions, isLoadingSpinnerVisible = false))
            }
        }
    }

    describe("ResultClicked Event") {
        val searchResult by memoized { SearchResult(1L, 0, 0, "name") }

        context("When result has COMMON tag") {
            beforeEachTest {
                searchResult.toggleTag(COMMON)
                searchPlantNameViewModel.onEvent(ResultClicked(searchResult))
            }

            it("Should set vernacularId") {
                searchPlantNameViewModel.vernacularId shouldEqual 1L
            }
        }

        context("When result has SCIENTIFIC tag") {
            beforeEachTest {
                searchResult.toggleTag(SCIENTIFIC)
                searchPlantNameViewModel.onEvent(ResultClicked(searchResult))
            }

            it("Should set taxonId") {
                searchPlantNameViewModel.taxonId shouldEqual 1L
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

        beforeEachTest { searchPlantNameViewModel.onEvent(ResultClicked(searchResult)) }

        it("Should emit NavigateToNext ViewAction") {
            verify(exactly = 1) { viewActionObserver.onChanged(NavigateToNext) }
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

        it("Should emit correct ViewStates/ViewActions") {
            verifyOrder {
                viewStateObserver.onChanged(ViewState(searchEditText = "string"))
                viewStateObserver.onChanged(ViewState(searchEditText = "string", isLoadingSpinnerVisible = true))
                viewActionObserver.onChanged(UpdateSearchResults(emptyList()))
                viewStateObserver.onChanged(ViewState(searchEditText = "string", isLoadingSpinnerVisible = false))
            }
        }

        context("When search text is same as previous") {
            beforeEachTest {
                searchPlantNameViewModel.onEvent(SearchEditTextChanged("string"))
                searchPlantNameViewModel.onEvent(SearchEditTextChanged("string"))
            }

            it("Should perform search only once") {
                verify(exactly = 1) { plantNameSearchService.search("string") }
            }
        }
    }

    describe("ClearSearchClicked Event") {
        beforeEachTest {
            searchPlantNameViewModel.vernacularId = 1L
            searchPlantNameViewModel.taxonId = 1L
            searchPlantNameViewModel.onEvent(ClearSearchClicked)
        }

        it("Should emit ClearSearchText ViewAction") {
            verify(exactly = 1) { viewActionObserver.onChanged(ClearSearchText) }
        }

        it("Should set vernacularId/taxonId to null") {
            searchPlantNameViewModel.vernacularId shouldEqual null
            searchPlantNameViewModel.taxonId shouldEqual null
        }
    }

    describe("SkipClicked Event") {
        beforeEachTest { searchPlantNameViewModel.onEvent(SkipClicked) }

        it("Should emit NavigateToNext ViewAction") {
            verify(exactly = 1) { viewActionObserver.onChanged(NavigateToNext) }
        }
    }
})