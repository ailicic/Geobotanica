package com.geobotanica.geobotanica.viewmodel

import android.content.Context
import androidx.lifecycle.Observer
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant.Type.SHRUB
import com.geobotanica.geobotanica.data.entity.Plant.Type.TREE
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.entity.Taxon
import com.geobotanica.geobotanica.data_taxa.entity.Vernacular
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.ui.newplantname.NewPlantNameViewModel
import com.geobotanica.geobotanica.ui.newplantname.ViewEffect
import com.geobotanica.geobotanica.ui.newplantname.ViewEffect.*
import com.geobotanica.geobotanica.ui.newplantname.ViewEffect.NavViewEffect.NavigateToNewPlantMeasurement
import com.geobotanica.geobotanica.ui.newplantname.ViewEvent.*
import com.geobotanica.geobotanica.ui.newplantname.ViewState
import com.geobotanica.geobotanica.util.MockkUtil.coVerifyOne
import com.geobotanica.geobotanica.util.MockkUtil.coVerifyZero
import com.geobotanica.geobotanica.util.MockkUtil.mockkObserver
import com.geobotanica.geobotanica.util.MockkUtil.verifyOne
import com.geobotanica.geobotanica.util.MockkUtil.verifyZero
import com.geobotanica.geobotanica.util.SpekExt.allowLiveData
import com.geobotanica.geobotanica.util.SpekExt.setupTestDispatchers
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe


object NewPlantNameViewModelTest : Spek({
    allowLiveData()
    val testDispatchers = setupTestDispatchers()

    val viewStateObserver = mockkObserver<ViewState>()
    val viewEffectObserver = mockkObserver<ViewEffect>()

    val appContext = mockk<Context> {
        every { resources.getString(R.string.suggested_common) } returns "Suggested common"
        every { resources.getString(R.string.suggested_scientific) } returns "Suggested scientific"
        every { resources.getInteger(R.integer.fragmentAnimTime) } returns 0 // TODO: Need to inject TestCoroutineScope in NewPlantNameViewModel if non-zero
    }

    val taxon1 = Taxon(generic = "Taxon1").apply { id = 1L }
    val taxon2 = Taxon(generic = "Taxon2").apply { id = 2L }
    val taxonRepo = mockk<TaxonRepo> {
        coEvery { this@mockk.get(taxon1.id) } returns taxon1
        coEvery { this@mockk.get(taxon2.id) } returns taxon2
        coEvery { this@mockk.setTagged(any(), any(), any()) } returns Unit
    }

    val vernacular1 = Vernacular(vernacular = "Vernacular1").apply { id = 1L }
    val vernacular2 = Vernacular(vernacular = "Vernacular2").apply { id = 2L }
    val vernacularRepo = mockk<VernacularRepo> {
        coEvery { this@mockk.get(vernacular1.id) } returns vernacular1
        coEvery { this@mockk.get(vernacular2.id) } returns vernacular2
        coEvery { this@mockk.setTagged(any(), any(), any()) } returns Unit
    }

    val tSearchResult1 = SearchResult(taxon1.id, SCIENTIFIC.flag, 0, taxon1.generic ?: "")
    val tSearchResult2 = SearchResult(taxon2.id, SCIENTIFIC.flag, 0, taxon2.generic ?: "")
    val tSearchResults = listOf(tSearchResult1, tSearchResult2)

    val vSearchResult1 = SearchResult(vernacular1.id, COMMON.flag, 0, vernacular1.vernacular ?: "")
    val vSearchResult2 = SearchResult(vernacular2.id, COMMON.flag, 0, vernacular2.vernacular ?: "")
    val vSearchResults = listOf(vSearchResult1, vSearchResult2)

    val plantNameSearchService = mockk<PlantNameSearchService> {
        every { searchSuggestedCommonNames(taxon1.id) } returns flowOf(vSearchResults)
        every { searchSuggestedScientificNames(vernacular1.id) } returns flowOf(tSearchResults)
    }

    val newPlantNameViewModel by memoized {
        NewPlantNameViewModel(testDispatchers, appContext, taxonRepo, vernacularRepo, plantNameSearchService).apply {
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

        context("Null ids") {
            beforeEachTest { newPlantNameViewModel.onEvent(ViewCreated(userId, photoUri, null, null)) }

            it("Should emit InitView effect once") { verifyOne { viewEffectObserver.onChanged(InitView) } }
            it("Should reset ViewState") { verify { viewStateObserver.onChanged(ViewState()) } }
            it("Should not query db") {
                coVerifyZero { vernacularRepo.get(any()) }
                coVerifyZero { taxonRepo.get(any()) }
                coVerifyZero { plantNameSearchService.searchSuggestedCommonNames(any()) }
                coVerifyZero { plantNameSearchService.searchSuggestedScientificNames(any()) }
            }
        }

        context("Non-null taxonId") {
            beforeEachTest {
                newPlantNameViewModel.onEvent(ViewCreated(userId, photoUri, taxon1.id, null))
            }

            it("Should emit correct ViewStates") {
                verifySequence {
                    viewStateObserver.onChanged(ViewState())
                    viewStateObserver.onChanged(ViewState(
                            isCommonNameEditable = true,
                            isScientificNameEditable = false,
                            suggestedText = "Suggested common"
                    ))
                    viewStateObserver.onChanged(ViewState(
                            isCommonNameEditable = true,
                            isScientificNameEditable = false,
                            commonName = "",
                            scientificName = taxon1.generic ?: "",
                            suggestedText = "Suggested common",
                            searchResults = vSearchResults
                    ))
                }
            }
            it("Should emit correct ViewEffects") {
                verifySequence {
                    viewEffectObserver.onChanged(InitView)
                    viewEffectObserver.onChanged(ShowScientificNameAnimation(taxon1.generic ?: ""))
                }
            }
        }

        context("Non-null vernacularId") {
            beforeEachTest {
                newPlantNameViewModel.onEvent(ViewCreated(userId, photoUri, null, vernacular1.id))
            }

            it("Should emit correct ViewStates") {
                verifySequence {
                    viewStateObserver.onChanged(ViewState())
                    viewStateObserver.onChanged(ViewState(
                            isCommonNameEditable = false,
                            isScientificNameEditable = true,
                            suggestedText = "Suggested scientific"
                    ))
                    viewStateObserver.onChanged(ViewState(
                            isCommonNameEditable = false,
                            isScientificNameEditable = true,
                            commonName = vernacular1.vernacular ?: "",
                            scientificName = "",
                            suggestedText = "Suggested scientific",
                            searchResults = tSearchResults
                    ))
                }
            }

            it("Should emit correct ViewEffects") {
                verifySequence {
                    viewEffectObserver.onChanged(InitView)
                    viewEffectObserver.onChanged(ShowCommonNameAnimation(vernacular1.vernacular ?: ""))
                }
            }
        }
    }

    describe("StarClicked Event") {

        context("Taxon") {
            beforeEachTest { newPlantNameViewModel.onEvent(StarClicked(tSearchResult1)) }

            it("Should update star") {
                coVerifyOne { taxonRepo.setTagged(taxon1.id, STARRED, false) }
            }
        }

        context("Vernacular") {
            beforeEachTest { newPlantNameViewModel.onEvent(StarClicked(vSearchResult1)) }

            it("Should update star") {
                coVerifyOne { vernacularRepo.setTagged(vernacular1.id, STARRED, false) }
            }
        }
    }

    describe("ResultClicked Event") {

        context("From taxon list") {
            beforeEachTest {
                newPlantNameViewModel.onEvent(ViewCreated(userId, photoUri, null, vernacular1.id))
            }

            context("Different result from last") {
                beforeEachTest {
                    newPlantNameViewModel.onEvent(ResultClicked(0, tSearchResult1))
                    newPlantNameViewModel.onEvent(ResultClicked(1, tSearchResult2))
                }

                it("Should emit correct ViewStates") {
                    verifyOrder {
                        viewStateObserver.onChanged(ViewState(
                                isCommonNameEditable = false,               // From ViewCreated
                                isScientificNameEditable = true,            // From ViewCreated
                                commonName = vernacular1.vernacular ?: "",  // From ViewCreated
                                scientificName = tSearchResult1.plantName,  // Important
                                suggestedText = "Suggested scientific",     // From ViewCreated
                                searchResults = tSearchResults,             // From ViewCreated
                                lastClickedResult = tSearchResult1,         // Important
                                lastClickedResultIndex = 0                  // Important
                        ))
                        viewStateObserver.onChanged(ViewState(
                                isCommonNameEditable = false,               // From ViewCreated
                                isScientificNameEditable = true,            // From ViewCreated
                                commonName = vernacular1.vernacular ?: "",  // From ViewCreated
                                scientificName = tSearchResult2.plantName,  // Important
                                suggestedText = "Suggested scientific",     // From ViewCreated
                                searchResults = tSearchResults,             // From ViewCreated
                                lastClickedResult = tSearchResult2,         // Important
                                lastClickedResultIndex = 1                  // Important
                        ))
                    }
                }

                it("Should animate both names") {
                    verifyOrder {
                        viewEffectObserver.onChanged(ShowScientificNameAnimation(tSearchResult1.plantName))
                        viewEffectObserver.onChanged(ShowScientificNameAnimation(tSearchResult2.plantName))
                    }
                }
            }

            context("Same result as last") {
                beforeEachTest {
                    newPlantNameViewModel.onEvent(ResultClicked(0, tSearchResult1))
                    newPlantNameViewModel.onEvent(ResultClicked(0, tSearchResult1))
                }

                it("Should emit correct ViewState") {
                    verify(exactly = 2) {
                        viewStateObserver.onChanged(ViewState(
                                isCommonNameEditable = false,               // From ViewCreated
                                isScientificNameEditable = true,            // From ViewCreated
                                commonName = vernacular1.vernacular ?: "",  // From ViewCreated
                                scientificName = tSearchResult1.plantName,  // Important
                                suggestedText = "Suggested scientific",     // From ViewCreated
                                searchResults = tSearchResults,             // From ViewCreated
                                lastClickedResult = tSearchResult1,         // Important
                                lastClickedResultIndex = 0                  // Important
                        ))
                    }
                }

                it("Should animate only once") {
                    verifyOne { viewEffectObserver.onChanged(ShowScientificNameAnimation(tSearchResult1.plantName)) }
                }
            }
        }

        context("From vernacular list") {

            beforeEachTest {
                newPlantNameViewModel.onEvent(ViewCreated(userId, photoUri, taxon1.id, null))
            }

            context("Different result from last") {
                beforeEachTest {
                    newPlantNameViewModel.onEvent(ResultClicked(0, vSearchResult1))
                    newPlantNameViewModel.onEvent(ResultClicked(1, vSearchResult2))
                }

                it("Should emit correct ViewStates") {
                    verifyOrder {
                        viewStateObserver.onChanged(ViewState(
                                isCommonNameEditable = true,            // From ViewCreated
                                isScientificNameEditable = false,       // From ViewCreated
                                commonName = vSearchResult1.plantName,  // Important
                                scientificName = taxon1.generic ?: "",  // From ViewCreated
                                suggestedText = "Suggested common",     // From ViewCreated
                                searchResults = vSearchResults,         // From ViewCreated
                                lastClickedResult = vSearchResult1,     // Important
                                lastClickedResultIndex = 0              // Important
                        ))
                        viewStateObserver.onChanged(ViewState(
                                isCommonNameEditable = true,            // From ViewCreated
                                isScientificNameEditable = false,       // From ViewCreated
                                commonName = vSearchResult2.plantName,  // Important
                                scientificName = taxon1.generic ?: "",  // From ViewCreated
                                suggestedText = "Suggested common",     // From ViewCreated
                                searchResults = vSearchResults,         // From ViewCreated
                                lastClickedResult = vSearchResult2,     // Important
                                lastClickedResultIndex = 1              // Important
                        ))
                    }
                }

                it("Should animate both names") {
                    verifyOrder {
                        viewEffectObserver.onChanged(ShowCommonNameAnimation(vSearchResult1.plantName))
                        viewEffectObserver.onChanged(ShowCommonNameAnimation(vSearchResult2.plantName))
                    }
                }
            }

            context("Same result as last") {
                beforeEachTest {
                    newPlantNameViewModel.onEvent(ResultClicked(0, vSearchResult1))
                    newPlantNameViewModel.onEvent(ResultClicked(0, vSearchResult1))
                }

                it("Should emit correct ViewStates") {
                    verify(exactly = 2) {
                        viewStateObserver.onChanged(ViewState(
                                isCommonNameEditable = true,            // From ViewCreated
                                isScientificNameEditable = false,       // From ViewCreated
                                commonName = vSearchResult1.plantName,  // Important
                                scientificName = taxon1.generic ?: "",  // From ViewCreated
                                suggestedText = "Suggested common",     // From ViewCreated
                                searchResults = vSearchResults,         // From ViewCreated
                                lastClickedResult = vSearchResult1,     // Important
                                lastClickedResultIndex = 0              // Important
                        ))
                    }
                }

                it("Should animate only once") {
                    verifyOne { viewEffectObserver.onChanged(ShowCommonNameAnimation(vSearchResult1.plantName)) }
                }
            }
        }
    }

    describe("CommonEditTextChanged Event") {

        beforeEachTest {
            newPlantNameViewModel.onEvent(ViewCreated(userId, photoUri, taxon1.id, null))
            newPlantNameViewModel.onEvent(ResultClicked(0, vSearchResult1))
        }

        context("Text same as last clicked") {
            beforeEachTest { newPlantNameViewModel.onEvent(CommonEditTextChanged(vernacular1.vernacular ?: "")) }

            it("Should emit correct ViewStates") {
                verify(exactly = 2) {
                    viewStateObserver.onChanged(ViewState(
                            isCommonNameEditable = true,            // From ViewCreated
                            isScientificNameEditable = false,       // From ViewCreated
                            commonName = vSearchResult1.plantName,  // Important
                            scientificName = taxon1.generic ?: "",  // From ViewCreated
                            suggestedText = "Suggested common",     // From ViewCreated
                            searchResults = vSearchResults,         // From ViewCreated
                            lastClickedResult = vSearchResult1,     // Important
                            lastClickedResultIndex = 0,             // From ResultClicked
                            isLastClickedShown = true               // Important
                    ))
                }
            }
        }

        context("Text not same as last clicked") {
            beforeEachTest { newPlantNameViewModel.onEvent(CommonEditTextChanged("not same")) }

            it("Should emit correct ViewStates") {
                verifyOrder {
                    viewStateObserver.onChanged(ViewState(
                            isCommonNameEditable = true,            // From ViewCreated
                            isScientificNameEditable = false,       // From ViewCreated
                            commonName = vSearchResult1.plantName,  // Important
                            scientificName = taxon1.generic ?: "",  // From ViewCreated
                            suggestedText = "Suggested common",     // From ViewCreated
                            searchResults = vSearchResults,         // From ViewCreated
                            lastClickedResult = vSearchResult1,     // Important
                            lastClickedResultIndex = 0,             // From ResultClicked
                            isLastClickedShown = true               // Important
                    ))
                    viewStateObserver.onChanged(ViewState(
                            isScientificNameEditable = false,       // From ViewCreated
                            isCommonNameEditable = true,            // From ViewCreated
                            commonName = "not same",                // Important
                            scientificName = taxon1.generic ?: "",  // From ViewCreated
                            suggestedText = "Suggested common",     // From ViewCreated
                            searchResults = vSearchResults,         // From ViewCreated
                            lastClickedResult = vSearchResult1,     // Important
                            lastClickedResultIndex = 0,             // From ResultClicked
                            isLastClickedShown = false              // Important
                    ))
                }
            }
        }
    }

    describe("ScientificEditTextChanged Event") {

        beforeEachTest {
            newPlantNameViewModel.onEvent(ViewCreated(userId, photoUri, null, vernacular1.id))
            newPlantNameViewModel.onEvent(ResultClicked(0, tSearchResult1))
        }

        context("Text same as last clicked") {
            beforeEachTest { newPlantNameViewModel.onEvent(ScientificEditTextChanged(taxon1.generic ?: "")) }

            it("Should emit correct ViewStates") {
                verify(exactly = 2) {
                    viewStateObserver.onChanged(ViewState(
                            isCommonNameEditable = false,               // From ViewCreated
                            isScientificNameEditable = true,            // From ViewCreated
                            commonName = vernacular1.vernacular ?: "",  // From ViewCreated
                            scientificName = tSearchResult1.plantName,  // Important
                            suggestedText = "Suggested scientific",     // From ViewCreated
                            searchResults = tSearchResults,             // From ViewCreated
                            lastClickedResult = tSearchResult1,         // Important
                            lastClickedResultIndex = 0,                 // From ResultClicked
                            isLastClickedShown = true                   // Important
                    ))
                }
            }
        }

        context("Text not same as last clicked") {
            beforeEachTest { newPlantNameViewModel.onEvent(ScientificEditTextChanged("not same")) }

            it("Should emit correct ViewStates") {
                verifyOrder {
                    viewStateObserver.onChanged(ViewState(
                            isCommonNameEditable = false,               // From ViewCreated
                            isScientificNameEditable = true,            // From ViewCreated
                            commonName = vernacular1.vernacular ?: "",  // From ViewCreated
                            scientificName = tSearchResult1.plantName,  // Important
                            suggestedText = "Suggested scientific",     // From ViewCreated
                            searchResults = tSearchResults,             // From ViewCreated
                            lastClickedResult = tSearchResult1,         // Important
                            lastClickedResultIndex = 0,                 // From ResultClicked
                            isLastClickedShown = true                   // Important
                    ))
                    viewStateObserver.onChanged(ViewState(
                            isCommonNameEditable = false,               // From ViewCreated
                            isScientificNameEditable = true,            // From ViewCreated
                            commonName = vernacular1.vernacular ?: "",  // From ViewCreated
                            scientificName = "not same",                // Important
                            suggestedText = "Suggested scientific",     // From ViewCreated
                            searchResults = tSearchResults,             // From ViewCreated
                            lastClickedResult = tSearchResult1,         // Important
                            lastClickedResultIndex = 0,                 // From ResultClicked
                            isLastClickedShown = false                  // Important
                    ))
                }
            }
        }
    }

    describe("FabClicked Event") {

        context("Both names empty") {
            beforeEachTest {
                newPlantNameViewModel.onEvent(ViewCreated(userId, photoUri, null, null))
                newPlantNameViewModel.onEvent(FabClicked)
            }

            it("Should emit ShowPlantNameSnackbar ViewEffect") {
                verifyOne { viewEffectObserver.onChanged(ShowPlantNameSnackbar) }
            }
        }

        context("One name non-empty") {

            beforeEachTest {
                newPlantNameViewModel.onEvent(ViewCreated(userId, photoUri, taxon1.id, null))
            }

            context("Single PlantType") {
                beforeEachTest {
                    coEvery { taxonRepo.getTypes(taxon1.id) } returns TREE.flag
                    newPlantNameViewModel.onEvent(FabClicked)
                }

                it("Should emit NavigateToNewPlantMeasurement ViewEffect") {
                    verifyOne {
                        viewEffectObserver.onChanged(NavigateToNewPlantMeasurement(
                                userId, photoUri, taxon1.id, null, TREE.flag
                        ))
                    }
                }

                it("Should not emit ShowPlantNameSnackbar ViewEffect") {
                    verifyZero { viewEffectObserver.onChanged(ShowPlantNameSnackbar) }
                }
            }

            context("Multiple PlantTypes") {
                beforeEachTest {
                    coEvery { taxonRepo.getTypes(taxon1.id) } returns (TREE.flag or SHRUB.flag)
                    newPlantNameViewModel.onEvent(FabClicked)
                }

                it("Should emit NavigateToNewPlantMeasurement ViewEffect") {
                    verifyOne {
                        viewEffectObserver.onChanged(NavViewEffect.NavigateToNewPlantType(
                                userId, photoUri, taxon1.id, null, (TREE.flag or SHRUB.flag)
                        ))
                    }
                }

                it("Should not emit ShowPlantNameSnackbar ViewEffect") {
                    verifyZero { viewEffectObserver.onChanged(ShowPlantNameSnackbar) }
                }
            }
        }
    }
})
