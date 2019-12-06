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
import com.geobotanica.geobotanica.ui.searchplantname.ViewEffect.*
import com.geobotanica.geobotanica.ui.searchplantname.ViewEvent.*
import com.geobotanica.geobotanica.util.GbDispatchers
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SingleLiveEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchPlantNameViewModel @Inject constructor (
        private val dispatchers: GbDispatchers,
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo,
        private val plantNameSearchService: PlantNameSearchService
): ViewModel() {
    var userId = 0L
    var photoUri: String = ""
    var taxonId: Long? = null
    var vernacularId: Long? = null

    private val _viewState = MutableLiveData<ViewState>().apply { value = ViewState() }
    val viewState: LiveData<ViewState> = _viewState

    private val _viewEffect = SingleLiveEvent<ViewEffect>()
    val viewEffect: LiveData<ViewEffect> = _viewEffect

    private var searchJob: Job? = null

    fun onEvent(event: ViewEvent) = when (event) {
        is ViewCreated -> {
            Lg.v("onEvent(ViewCreated)")
            taxonId = null
            vernacularId = null
            _viewState.value = ViewState()
            triggerViewEffect(InitView)
        }
        is OnStart -> {
            Lg.v("onEvent(OnStart)")
            val searchEditText = event.searchEditText
            updateViewState(searchEditText = searchEditText)
            updateSearchResults()
        }
        is SearchFilterSelected -> {
            Lg.v("onEvent(SearchFilterSelected)")
            val searchFilterOptions = event.searchFilterOptions
            if (searchFilterOptions.filterFlags != viewState.value?.searchFilterOptions?.filterFlags) {
                triggerViewEffect(UpdateSharedPrefs(searchFilterOptions))
                updateViewState(searchFilterOptions = searchFilterOptions)
                updateSearchResults()
            }
            Unit
        }
        is ResultClicked -> {
            Lg.v("onEvent(ResultClicked)")
            val result = event.searchResult
            if (result.hasTag(SCIENTIFIC))
                taxonId = result.id
            else if (result.hasTag(COMMON))
                vernacularId = result.id
            if (result.hasTag(STARRED))
                updateStarredTimestamp(result)
            triggerViewEffect(NavigateToNext)
        }
        is StarClicked -> { updateIsStarred(event.searchResult); Unit }
        is SearchEditTextChanged -> {
            Lg.v("onEvent(SearchEditTextChanged)")
            val editText = event.searchEditText
            if (viewState.value?.searchEditText != editText) {
                updateViewState(searchEditText = editText)
                updateSearchResults()
            }
            Unit
        }
        is ClearSearchClicked -> {
            taxonId = null
            vernacularId = null
            triggerViewEffect(ClearSearchText)
        }
        is SkipClicked -> triggerViewEffect(NavigateToNext)
    }

    private fun updateSearchResults() {
        updateViewState(isNoResultsTextVisible = false)
        searchJob?.cancel()
        searchJob = viewModelScope.launch(dispatchers.main) {
            delay(300)
            updateViewState(isLoadingSpinnerVisible = true)
            val searchEditText = viewState.value!!.searchEditText
            val searchFilterOptions = viewState.value!!.searchFilterOptions
            val showStars = ! searchFilterOptions.hasFilter(STARRED)
            plantNameSearchService.search(searchEditText, searchFilterOptions).collect {
                triggerViewEffect(UpdateSearchResults(it, showStars))
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

    private fun updateIsStarred(result: SearchResult) = viewModelScope.launch(dispatchers.io) {
        when {
            result.hasTag(SCIENTIFIC) -> taxonRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
            result.hasTag(COMMON) -> vernacularRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
        }
    }

    private fun updateStarredTimestamp(result: SearchResult) = viewModelScope.launch(dispatchers.io) {
        when {
            result.hasTag(SCIENTIFIC) -> taxonRepo.updateTagTimestamp(result.id, STARRED)
            result.hasTag(COMMON) -> vernacularRepo.updateTagTimestamp(result.id, STARRED)
        }
    }

    private fun updateViewState(
            isNoResultsTextVisible: Boolean = viewState.value?.isNoResultsTextVisible ?: false,
            isLoadingSpinnerVisible: Boolean = viewState.value?.isLoadingSpinnerVisible ?: false,
            searchFilterOptions: SearchFilterOptions = viewState.value?.searchFilterOptions ?: SearchFilterOptions(),
            searchEditText: String = viewState.value?.searchEditText ?: "",
            searchResults: List<SearchResult> = viewState.value?.searchResults ?: emptyList()
    ) {
        _viewState.value = viewState.value?.copy(
                isNoResultsTextVisible = isNoResultsTextVisible,
                isLoadingSpinnerVisible = isLoadingSpinnerVisible,
                searchFilterOptions = searchFilterOptions,
                searchEditText = searchEditText,
                searchResults = searchResults
        )
    }

    private fun triggerViewEffect(viewEffect: ViewEffect) {
        _viewEffect.value = viewEffect
    }
}

data class ViewState(
        val isNoResultsTextVisible: Boolean = false,
        val isLoadingSpinnerVisible: Boolean = false,
        val searchFilterOptions: SearchFilterOptions = SearchFilterOptions(), // Not used in render()
        val searchEditText: String = "", // Not used in render()
        val searchResults: List<SearchResult> = emptyList() // Not used in render()
)

sealed class ViewEvent {
    object ViewCreated : ViewEvent()
    data class OnStart(val searchEditText: String) : ViewEvent()
    data class SearchFilterSelected(val searchFilterOptions: SearchFilterOptions) : ViewEvent()
    data class ResultClicked(val searchResult: SearchResult) : ViewEvent()
    data class StarClicked(val searchResult: SearchResult) : ViewEvent()
    data class SearchEditTextChanged(val searchEditText: String) : ViewEvent()
    object ClearSearchClicked : ViewEvent()
    object SkipClicked : ViewEvent()
}

sealed class ViewEffect {
    object InitView : ViewEffect()
    data class UpdateSearchResults(val searchResults: List<SearchResult>, val showStars: Boolean = true) : ViewEffect()
    object ClearSearchText : ViewEffect()
    data class UpdateSharedPrefs(val searchFilterOptions: SearchFilterOptions) : ViewEffect()
    object NavigateToNext : ViewEffect()
}