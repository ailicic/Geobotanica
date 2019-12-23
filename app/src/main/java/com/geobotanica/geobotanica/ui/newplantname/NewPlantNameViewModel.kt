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
import com.geobotanica.geobotanica.ui.newplantname.ViewEffect.NavViewEffect.*
import com.geobotanica.geobotanica.ui.newplantname.ViewEvent.*
import com.geobotanica.geobotanica.util.GbDispatchers
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SingleLiveEvent
import com.geobotanica.geobotanica.util.mutableLiveData
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class NewPlantNameViewModel @Inject constructor (
        private val dispatchers: GbDispatchers,
        private val appContext: Context,
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo,
        private val plantNameSearchService: PlantNameSearchService
): ViewModel() {

    private val _viewState = mutableLiveData(ViewState())
    val viewState: LiveData<ViewState> = _viewState

    private val _viewEffect = SingleLiveEvent<ViewEffect>()
    val viewEffect: LiveData<ViewEffect> = _viewEffect

    // Bundle data
    private var userId = 0L
    private var photoUri: String = ""
    private var taxonId: Long? = null
    private var vernacularId: Long? = null

    fun onEvent(event: ViewEvent): Unit = when (event) {
        is ViewCreated -> {
            Lg.v("onEvent(ViewCreated)")

            userId = event.userId
            photoUri = event.photoUri
            taxonId = event.taxonId
            vernacularId = event.vernacularId
            Lg.d("Fragment args: userId=$userId, vernId=$vernacularId, taxonId=$taxonId, photoUri=$photoUri")

            when {
                taxonId != null -> {
                    updateViewState(
                            isCommonNameEditable = true,
                            isScientificNameEditable = false,
                            commonName = "",
                            scientificName = "",
                            suggestedText = appContext.resources.getString(R.string.suggested_common),
                            lastClickedResultIndex = null
                    )
                }
                vernacularId != null -> {
                    updateViewState(
                            isCommonNameEditable = false,
                            isScientificNameEditable = true,
                            commonName = "",
                            scientificName = "",
                            suggestedText = appContext.resources.getString(R.string.suggested_scientific),
                            lastClickedResultIndex = null
                    )
                }
                else -> _viewState.value = ViewState()
            }
            emitViewEffect(InitView)
            loadPlantNames()
        }
        is CommonEditTextChanged -> {
            Lg.v("onEvent(CommonEditTextChanged)")
            val editText = event.editText
            val lastClickedResult = viewState.value?.lastClickedResult
            val isTextSame = editText == lastClickedResult?.plantName ?: true
            updateViewState(commonName = editText, isLastClickedShown = isTextSame)
            vernacularId = if (isTextSame) lastClickedResult?.id else null
        }
        is ScientificEditTextChanged -> {
            Lg.v("onEvent(ScientificEditTextChanged)")
            val editText = event.editText
            val lastClickedResult = viewState.value?.lastClickedResult
            val isTextSame = editText == lastClickedResult?.plantName ?: true
            updateViewState(scientificName = editText, isLastClickedShown = isTextSame)
            taxonId = if (isTextSame) lastClickedResult?.id else null
        }
        is StarClicked -> { updateIsStarred(event.searchResult); Unit }
        is ResultClicked -> viewState.value?.let { viewState ->
            Lg.v("onEvent(ResultClicked)")
            val index = event.index
            val searchResult = event.searchResult
            val isClickedResultSameAsLast = event.index == viewState.lastClickedResultIndex
            if (viewState.isCommonNameEditable) {
                vernacularId = searchResult.id
                updateViewState(
                        commonName = searchResult.plantName,
                        lastClickedResultIndex = index,
                        lastClickedResult = searchResult
                )
                if (! isClickedResultSameAsLast) {
                    emitViewEffect(ShowCommonNameAnimation(searchResult.plantName))
                }
            }
            if (viewState.isScientificNameEditable) {
                taxonId = searchResult.id
                updateViewState(
                        scientificName = searchResult.plantName,
                        lastClickedResultIndex = index,
                        lastClickedResult = searchResult
                )
                if (! isClickedResultSameAsLast)
                    emitViewEffect(ShowScientificNameAnimation(searchResult.plantName))
            }
        } ?: Unit
        is FabClicked -> viewState.value?.let { viewState ->
            Lg.v("onEvent(FabClicked)")
            if (viewState.commonName.isBlank() && viewState.scientificName.isBlank()) {
                emitViewEffect(ShowPlantNameSnackbar)
            } else {
                viewModelScope.launch(dispatchers.main) {
                    val plantTypeFlags = getPlantTypes()
                    if (! isSinglePlantType(plantTypeFlags))
                        emitViewEffect(NavigateToNewPlantType(userId, photoUri, taxonId, vernacularId, plantTypeFlags))
                    else
                        emitViewEffect(NavigateToNewPlantMeasurement(userId, photoUri, taxonId, vernacularId, plantTypeFlags))
                }
            }; Unit
        } ?: Unit
    }

    @SuppressLint("DefaultLocale")
    private fun loadPlantNames() {
        viewModelScope.launch(dispatchers.main) {
            delay(appContext.resources.getInteger(R.integer.fragmentAnimTime).toLong())
            taxonId?.let {
                val scientificName = taxonRepo.get(it)?.scientific?.capitalize() ?: ""
                plantNameSearchService.searchSuggestedCommonNames(it).collect { results ->
                    ensureActive()
                    updateViewState(commonName = "", scientificName = scientificName, searchResults = results)
                }
            }
            vernacularId?.let {
                val commonName = vernacularRepo.get(it)?.vernacular?.capitalize() ?: ""
                plantNameSearchService.searchSuggestedScientificNames(it).collect { results ->
                    ensureActive()
                    updateViewState(commonName = commonName, scientificName = "", searchResults = results)
                }
            }
        }.invokeOnCompletion { completionError ->
            if (completionError != null) // Coroutine did not complete
                return@invokeOnCompletion
            taxonId?.let { emitViewEffect(ShowScientificNameAnimation(viewState.value?.scientificName ?: "")) }
            vernacularId?.let { emitViewEffect(ShowCommonNameAnimation(viewState.value?.commonName ?: "")) }
        }
    }

    private fun updateIsStarred(result: SearchResult) = viewModelScope.launch(dispatchers.main) {
        when {
            result.hasTag(SCIENTIFIC) -> taxonRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
            result.hasTag(COMMON) -> vernacularRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
        }
    }

    private suspend fun getPlantTypes(): Int = withContext(dispatchers.io) {
        var plantTypeFlags = 0
        taxonId?.let { plantTypeFlags = taxonRepo.getTypes(it) } ?:
                vernacularId?.let { plantTypeFlags = vernacularRepo.getTypes(it) }
        plantTypeFlags
    }

    private fun isSinglePlantType(plantTypeFlags: Int): Boolean = Plant.Type.flagsToList(plantTypeFlags).size == 1

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

    private fun emitViewEffect(viewEffect: ViewEffect) {
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
    data class ViewCreated(val userId: Long, val photoUri: String, val taxonId: Long?, val vernacularId: Long?) : ViewEvent()
    data class CommonEditTextChanged(val editText: String) : ViewEvent()
    data class ScientificEditTextChanged(val editText: String) : ViewEvent()
    data class StarClicked(val searchResult: SearchResult) : ViewEvent()
    data class ResultClicked(val index: Int, val searchResult: SearchResult) : ViewEvent()
    object FabClicked : ViewEvent()
}

sealed class ViewEffect {
    object InitView : ViewEffect()
    data class ShowCommonNameAnimation(val name: String) : ViewEffect()
    data class ShowScientificNameAnimation(val name: String) : ViewEffect()
    object ShowPlantNameSnackbar : ViewEffect()

    sealed class NavViewEffect : BundleData, ViewEffect() {
        data class NavigateToNewPlantMeasurement(
                override val userId: Long,
                override val photoUri: String,
                override val taxonId: Long?,
                override val vernacularId: Long?,
                override val plantTypeFlags: Int
        ) : NavViewEffect()

        data class NavigateToNewPlantType(
                override val userId: Long,
                override val photoUri: String,
                override val taxonId: Long?,
                override val vernacularId: Long?,
                override val plantTypeFlags: Int
        ) : NavViewEffect()
    }

    interface BundleData { // Using an interface for NavViewEffect allows overriding in data classes (need equals() )
        val userId: Long
        val photoUri: String
        val vernacularId: Long?
        val taxonId: Long?
        val plantTypeFlags: Int
    }
}