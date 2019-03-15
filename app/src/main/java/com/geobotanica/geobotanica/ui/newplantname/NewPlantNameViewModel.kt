package com.geobotanica.geobotanica.ui.newplantname

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantNameViewModel @Inject constructor (
    private val taxonRepo: TaxonRepo,
    private val vernacularRepo: VernacularRepo,
    private val plantNameSearchService: PlantNameSearchService
): ViewModel() {
    var userId = 0L
    var plantType = Plant.Type.TREE
    var photoUri: String = ""
    var commonName: String? = null
    var scientificName: String? = null

    var taxonId: Long? = null
    var vernacularId: Long? = null

    var lastSelectedIndex: Int? = null
    var lastSelectedId: Long? = null
    var lastSelectedName: String = ""

    suspend fun loadNamesFromIds() = withContext(Dispatchers.IO) {
        commonName = null
        scientificName = null
        vernacularId?.let { commonName = vernacularRepo.get(it)?.vernacular?.capitalize() } ?:
        taxonId?.let { scientificName = taxonRepo.get(it)?.scientific?.capitalize() }
    }

    @ExperimentalCoroutinesApi
    fun searchSuggestedCommonNames(taxonId: Long): ReceiveChannel<List<SearchResult>> =
        plantNameSearchService.searchSuggestedCommonNames(taxonId)

    @ExperimentalCoroutinesApi
    fun searchSuggestedScientificNames(vernacularId: Long): ReceiveChannel<List<SearchResult>> =
        plantNameSearchService.searchSuggestedScientificNames(vernacularId)

    fun updateIsStarred(result: SearchResult) {
        GlobalScope.launch(Dispatchers.IO) {
            when {
                result.hasTag(COMMON) -> vernacularRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
                result.hasTag(SCIENTIFIC) -> taxonRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
            }
        }
    }
}
