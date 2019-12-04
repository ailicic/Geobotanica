package com.geobotanica.geobotanica.ui.newplantname

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.ui.newplantname.ViewEffect.*
import com.geobotanica.geobotanica.ui.newplantname.ViewEvent.*
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class NewPlantNameViewModel @Inject constructor (
        private val appContext: Context,
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo,
        private val plantNameSearchService: PlantNameSearchService
): ViewModel() {
    var userId = 0L
    var photoUri: String = ""

    var taxonId: Long? = null
    var vernacularId: Long? = null
    var plantTypes: Int? = null

    private val _viewState = MutableLiveData<ViewState>().apply { value = ViewState() }
    val viewState: LiveData<ViewState> = _viewState

    private val _viewEffect = SingleLiveEvent<ViewEffect>()
    val viewEffect: LiveData<ViewEffect> = _viewEffect


    fun onEvent(event: ViewEvent) = when (event) {
        is ViewCreated -> {
            Lg.v("onEvent(ViewCreated)")

            vernacularId = event.vernacularId
            taxonId = event.taxonId
            when {
                vernacularId != null -> {
                    updateViewState(
                            isCommonNameEditable = false,
                            isScientificNameEditable = true,
                            scientificName = "",
                            suggestedText = appContext.resources.getString(R.string.suggested_scientific),
                            lastClickedResultIndex = null
                    )
                }
                taxonId != null -> {
                    updateViewState(
                            isCommonNameEditable = true,
                            isScientificNameEditable = false,
                            commonName = "",
                            suggestedText = appContext.resources.getString(R.string.suggested_common),
                            lastClickedResultIndex = null
                    )
                }
                else -> _viewState.value = ViewState()
            }
            triggerViewEffect(InitView)
            loadPlantNames()
        }
        is CommonEditTextChanged -> {
            Lg.v("onEvent(CommonEditTextChanged)")
            val editText = event.editText
            val isLastClickedShown = editText == viewState.value?.lastClickedResult?.plantName ?: true
            updateViewState(commonName = editText, isLastClickedShown = isLastClickedShown)
            viewState.value?.lastClickedResult?.let {
                vernacularId = if (editText == it.plantName) it.id else null
            } ?: Unit
        }
        is ScientificEditTextChanged -> {
            Lg.v("onEvent(ScientificEditTextChanged)")
            val editText = event.editText
            val isLastClickedShown = editText == viewState.value?.lastClickedResult?.plantName ?: true
            updateViewState(scientificName = editText, isLastClickedShown = isLastClickedShown)
            viewState.value?.lastClickedResult?.let {
                taxonId = if (editText == it.plantName) it.id else null
            } ?: Unit
        }
        is StarClicked -> { updateIsStarred(event.searchResult); Unit }
        is ResultClicked -> {
            Lg.v("onEvent(ResultClicked)")
            val index = event.index
            val searchResult = event.searchResult
            val isClickedResultSameAsLast = event.index == viewState.value?.lastClickedResultIndex
            if (viewState.value!!.isCommonNameEditable) {
                vernacularId = searchResult.id
                updateViewState(
                        commonName = searchResult.plantName,
                        lastClickedResultIndex = index,
                        lastClickedResult = searchResult
                )
                if (! isClickedResultSameAsLast) {
                    triggerViewEffect(ShowCommonNameAnimation(searchResult.plantName))
                }
            }
            if (viewState.value!!.isScientificNameEditable) {
                taxonId = searchResult.id
                updateViewState(
                        scientificName = searchResult.plantName,
                        lastClickedResultIndex = index,
                        lastClickedResult = searchResult
                )
                if (! isClickedResultSameAsLast)
                    triggerViewEffect(ShowScientificNameAnimation(searchResult.plantName))
            }; Unit
        }
        is FabClicked -> {
            Lg.v("onEvent(FabClicked)")
            if (event.commonNameEditText.isBlank() && event.scientificNameEditText.isBlank()) {
                triggerViewEffect(ShowPlantNameSnackbar)
            } else {
                viewModelScope.launch {
                    getPlantTypes()
                    if (! isPlantTypeKnown())
                        triggerViewEffect(NavigateToNewPlantType)
                    else
                        triggerViewEffect(NavigateToNewPlantMeasurement)
                }
            }; Unit
        }
    }

    @SuppressLint("DefaultLocale")
    private fun loadPlantNames() {
        viewModelScope.launch {
            vernacularId?.let {
                val commonName = vernacularRepo.get(it)?.vernacular?.capitalize() ?: ""
                plantNameSearchService.searchSuggestedScientificNames(it).collect { results ->
                    updateViewState(commonName = commonName, scientificName = "", searchResults = results)
                }
            }
            taxonId?.let {
                val scientificName = taxonRepo.get(it)?.scientific?.capitalize() ?: ""
                plantNameSearchService.searchSuggestedCommonNames(it).collect { results ->
                    updateViewState(commonName = "", scientificName = scientificName, searchResults = results)
                }
            }
            delay(appContext.resources.getInteger(R.integer.fragmentAnimTime).toLong())
        }.invokeOnCompletion { completionError ->
            if (completionError != null) // Coroutine did not complete
                return@invokeOnCompletion
            vernacularId?.let { triggerViewEffect(ShowCommonNameAnimation(viewState.value?.commonName!!)) }
            taxonId?.let { triggerViewEffect(ShowScientificNameAnimation(viewState.value?.scientificName!!)) }
        }
    }

    private fun updateIsStarred(result: SearchResult) = viewModelScope.launch {
        when {
            result.hasTag(COMMON) -> vernacularRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
            result.hasTag(SCIENTIFIC) -> taxonRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
        }
    }

    private suspend fun getPlantTypes() = withContext(Dispatchers.IO) {
        taxonId?.let { plantTypes = taxonRepo.getTypes(it) } ?:
                vernacularId?.let { plantTypes = vernacularRepo.getTypes(it) }
    }

    private fun isPlantTypeKnown(): Boolean {
        return plantTypes?.let {
            Plant.Type.flagsToList(it).size == 1
        } ?: false
    }

    private fun updateViewState(
            isCommonNameEditable: Boolean = viewState.value?.isCommonNameEditable ?: true,
            isScientificNameEditable: Boolean = viewState.value?.isScientificNameEditable ?: true,
            commonName: String = viewState.value?.commonName ?: "",
            scientificName: String = viewState.value?.scientificName ?: "",
            suggestedText: String = viewState.value?.suggestedText ?: "",
            isLastClickedShown: Boolean = viewState.value?.isLastClickedShown ?: true,
            searchResults: List<SearchResult> = viewState.value?.searchResults ?: emptyList(),
            lastClickedResult: SearchResult? = viewState.value?.lastClickedResult,
            lastClickedResultIndex: Int? = viewState.value?.lastClickedResultIndex
    ) {
        _viewState.value = viewState.value?.copy(
                isCommonNameEditable = isCommonNameEditable,
                isScientificNameEditable = isScientificNameEditable,
                commonName = commonName,
                scientificName = scientificName,
                suggestedText = suggestedText,
                isLastClickedShown = isLastClickedShown,
                searchResults = searchResults,
                lastClickedResult = lastClickedResult,
                lastClickedResultIndex = lastClickedResultIndex
        )
    }

    private fun triggerViewEffect(viewEffect: ViewEffect) {
        _viewEffect.value = viewEffect
    }
}

data class ViewState(
        val isCommonNameEditable: Boolean = true,
        val isScientificNameEditable: Boolean = true,
        val commonName: String = "",
        val scientificName: String = "",
        val suggestedText: String = "",
        val searchResults: List<SearchResult> = emptyList(),
        val lastClickedResult: SearchResult? = null,
        val lastClickedResultIndex: Int? = null,
        val isLastClickedShown: Boolean = true
)

sealed class ViewEvent {
    data class ViewCreated(val vernacularId: Long?, val taxonId: Long?) : ViewEvent()
    data class CommonEditTextChanged(val editText: String) : ViewEvent()
    data class ScientificEditTextChanged(val editText: String) : ViewEvent()
    data class StarClicked(val searchResult: SearchResult) : ViewEvent()
    data class ResultClicked(val index: Int, val searchResult: SearchResult) : ViewEvent()
    data class FabClicked(val commonNameEditText: String, val scientificNameEditText: String) : ViewEvent()
}

sealed class ViewEffect {
    object InitView : ViewEffect()
    data class ShowCommonNameAnimation(val name: String) : ViewEffect()
    data class ShowScientificNameAnimation(val name: String) : ViewEffect()
    object NavigateToNewPlantMeasurement : ViewEffect()
    object NavigateToNewPlantType : ViewEffect()
    object ShowPlantNameSnackbar : ViewEffect()
}