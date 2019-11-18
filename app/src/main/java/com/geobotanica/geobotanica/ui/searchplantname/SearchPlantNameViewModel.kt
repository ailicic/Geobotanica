package com.geobotanica.geobotanica.ui.searchplantname

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchFilterOptions
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.ui.searchplantname.ViewAction.*
import com.geobotanica.geobotanica.ui.searchplantname.ViewEvent.*
import com.geobotanica.geobotanica.util.SingleLiveEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchPlantNameViewModel @Inject constructor (
    private val taxonRepo: TaxonRepo,
    private val vernacularRepo: VernacularRepo,
    private val plantNameSearchService: PlantNameSearchService
): ViewModel() {
    var userId = 0L
    var photoUri: String = ""
    var taxonId: Long? = null
    var vernacularId: Long? = null

    private val _viewState = MutableLiveData<ViewState>()
    val viewState: LiveData<ViewState> = _viewState

    private val _viewAction = SingleLiveEvent<ViewAction>()
    val viewAction: LiveData<ViewAction> = _viewAction

    private var searchJob: Job? = null

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }

    fun onEvent(event: ViewEvent) {
        when (event) {
            is ViewCreated -> {
                taxonId = null
                vernacularId = null
                triggerViewAction(InitView)
                _viewState.value = ViewState()
            }
            is SearchFilterSelected -> {
                val searchFilterOptions = event.searchFilterOptions
                if (searchFilterOptions.filterFlags != viewState.value?.searchFilterOptions?.filterFlags) {
                    triggerViewAction(UpdateSharedPrefs(searchFilterOptions))
                    updateViewState(searchFilterOptions = searchFilterOptions)
                    updateSearchResults()
                }

            }
            is ResultClicked -> {
                val result = event.searchResult
                if (result.hasTag(COMMON))
                    vernacularId = result.id
                else if (result.hasTag(SCIENTIFIC))
                    taxonId = result.id
                if (result.hasTag(STARRED))
                    updateStarredTimestamp(result)
                triggerViewAction(NavigateToNext)

            }
            is StarClicked -> updateIsStarred(event.searchResult)
            is SearchEditTextChanged -> {
                val editText = event.editText
                if (viewState.value?.searchEditText == editText)
                    return
                searchJob?.cancel()
                updateViewState(searchEditText = editText)
                updateSearchResults()
            }
            is ClearSearchClicked -> {
                taxonId = null
                vernacularId = null
                triggerViewAction(ClearSearchText)
            }
            is SkipClicked -> triggerViewAction(NavigateToNext)
        }
    }

    private fun updateSearchResults() {
        updateViewState(isNoResultsTextVisible = false)
        searchJob = viewModelScope.launch {
            delay(300)
            updateViewState(isLoadingSpinnerVisible = true)
            plantNameSearchService.search(_viewState.value!!.searchEditText, _viewState.value!!.searchFilterOptions).collect {
                triggerViewAction(UpdateSearchResults(it))
            }
        }
        searchJob?.invokeOnCompletion { completionError ->
            if (completionError != null) // Coroutine did not complete
                return@invokeOnCompletion
            updateViewState(isLoadingSpinnerVisible = false)

            if(viewState.value!!.searchResults.isEmpty())
                updateViewState(isNoResultsTextVisible = true)
        }
    }

    private fun updateIsStarred(result: SearchResult) = viewModelScope.launch {
        when {
            result.hasTag(COMMON) -> vernacularRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
            result.hasTag(SCIENTIFIC) -> taxonRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
        }
    }

    private fun updateStarredTimestamp(result: SearchResult) = viewModelScope.launch {
        when {
            result.hasTag(COMMON) -> vernacularRepo.updateTagTimestamp(result.id, STARRED)
            result.hasTag(SCIENTIFIC) -> taxonRepo.updateTagTimestamp(result.id, STARRED)
        }
    }

    private fun updateViewState(
            isNoResultsTextVisible: Boolean? = null,
            isLoadingSpinnerVisible: Boolean? = null,
            searchFilterOptions: SearchFilterOptions? = null,
            searchEditText: String? = null,
            searchResults: List<SearchResult>? = null
    ) {
        isNoResultsTextVisible?.let { _viewState.value = viewState.value?.copy(isNoResultsTextVisible = isNoResultsTextVisible) }
        isLoadingSpinnerVisible?.let { _viewState.value = viewState.value?.copy(isLoadingSpinnerVisible = isLoadingSpinnerVisible) }
        searchFilterOptions?.let { _viewState.value = viewState.value?.copy(searchFilterOptions = searchFilterOptions) }
        searchEditText?.let { _viewState.value = viewState.value?.copy(searchEditText = searchEditText) }
        searchResults?.let { _viewState.value = viewState.value?.copy(searchResults = searchResults) }
    }

    private fun triggerViewAction(viewAction: ViewAction) {
        _viewAction.value = viewAction
    }
}


sealed class ViewEvent {
    object ViewCreated : ViewEvent()
    data class SearchFilterSelected(val searchFilterOptions: SearchFilterOptions) : ViewEvent()
    data class ResultClicked(val searchResult: SearchResult) : ViewEvent()
    data class StarClicked(val searchResult: SearchResult) : ViewEvent()
    data class SearchEditTextChanged(val editText: String) : ViewEvent()
    object ClearSearchClicked : ViewEvent()
    object SkipClicked : ViewEvent()
}


sealed class ViewAction {
    object InitView : ViewAction()
    data class UpdateSearchResults(val searchResults: List<SearchResult>) : ViewAction()
    object ClearSearchText : ViewAction()
    data class UpdateSharedPrefs(val searchFilterOptions: SearchFilterOptions) : ViewAction()
    object NavigateToNext : ViewAction()
}

data class ViewState(
        val isNoResultsTextVisible: Boolean = false,
        val isLoadingSpinnerVisible: Boolean = false,
        val searchFilterOptions: SearchFilterOptions = SearchFilterOptions(), // Not used in render()
        val searchEditText: String = "", // Not used in render()
        val searchResults: List<SearchResult> = emptyList() // Not used in render()
)