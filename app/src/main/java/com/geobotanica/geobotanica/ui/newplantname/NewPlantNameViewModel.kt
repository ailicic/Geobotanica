package com.geobotanica.geobotanica.ui.newplantname

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchFilterOptions
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag.*
import com.geobotanica.geobotanica.util.Lg
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

    var searchText = ""
    lateinit var searchFilterOptions: SearchFilterOptions

    // TODO: Remove this init block (for testing)
    init {
        GlobalScope.launch(Dispatchers.IO) {
            Lg.d("Count vern: ${vernacularRepo.getCount()}")
            Lg.d("Count taxa: ${taxonRepo.getCount()}")
        }
    }

    @ExperimentalCoroutinesApi
    fun searchPlantName(string: String): ReceiveChannel<List<SearchResult>> =
        plantNameSearchService.search(string, searchFilterOptions)

    fun updateIsUsed(result: SearchResult) {
        GlobalScope.launch(Dispatchers.IO) {
            when {
                result.hasTag(COMMON) -> vernacularRepo.setUsed(result.id, result.hasTag(USED))
                result.hasTag(SCIENTIFIC) -> taxonRepo.setUsed(result.id, result.hasTag(USED))
                else -> { }
            }
        }
    }

    suspend fun getDefaultPlantNames(): List<SearchResult> = withContext(Dispatchers.IO) {
        plantNameSearchService.getDefault(searchFilterOptions)
    }

    fun updateIsStarred(result: SearchResult) {
        GlobalScope.launch(Dispatchers.IO) {
            when {
                result.hasTag(COMMON) -> vernacularRepo.setStarred(result.id, result.hasTag(STARRED))
                result.hasTag(SCIENTIFIC) -> taxonRepo.setStarred(result.id, result.hasTag(STARRED))
            }
        }
    }
}
