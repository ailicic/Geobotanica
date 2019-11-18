package com.geobotanica.geobotanica.ui.newplantname

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantNameViewModel @Inject constructor (
    private val taxonRepo: TaxonRepo,
    private val vernacularRepo: VernacularRepo,
    private val plantNameSearchService: PlantNameSearchService
): ViewModel() {
    var userId = 0L
    var photoUri: String = ""
    var commonName: String? = null
    var scientificName: String? = null

    var taxonId: Long? = null
    var vernacularId: Long? = null
    var plantTypes: Int? = null

    var lastSelectedIndex: Int? = null
    var lastSelectedId: Long? = null
    var lastSelectedName: String = ""

    @SuppressLint("DefaultLocale")
    suspend fun loadNamesFromIds() = withContext(Dispatchers.IO) {
        commonName = null
        scientificName = null
        vernacularId?.let { commonName = vernacularRepo.get(it)?.vernacular?.capitalize() } ?:
        taxonId?.let { scientificName = taxonRepo.get(it)?.scientific?.capitalize() }
    }

    fun searchSuggestedCommonNames(taxonId: Long): Flow<List<SearchResult>> =
        plantNameSearchService.searchSuggestedCommonNames(taxonId)

    fun searchSuggestedScientificNames(vernacularId: Long): Flow<List<SearchResult>> =
        plantNameSearchService.searchSuggestedScientificNames(vernacularId)

    fun updateIsStarred(result: SearchResult) = viewModelScope.launch {
        when {
            result.hasTag(COMMON) -> vernacularRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
            result.hasTag(SCIENTIFIC) -> taxonRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
        }
    }

    suspend fun getPlantTypes() = withContext(Dispatchers.IO) {
        taxonId?.let {
            plantTypes = taxonRepo.getTypes(it)
        } ?: vernacularId?.let {
            plantTypes = vernacularRepo.getTypes(it)
        }
    }

    fun isPlantTypeKnown(): Boolean {
        return plantTypes?.let {
            Plant.Type.flagsToList(it).size == 1
        } ?: false
    }
}
