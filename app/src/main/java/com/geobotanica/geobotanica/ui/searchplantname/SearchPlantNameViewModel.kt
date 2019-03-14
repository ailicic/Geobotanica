package com.geobotanica.geobotanica.ui.searchplantname

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
class SearchPlantNameViewModel @Inject constructor (
    private val taxonRepo: TaxonRepo,
    private val vernacularRepo: VernacularRepo,
    private val plantNameSearchService: PlantNameSearchService
): ViewModel() {
    var userId = 0L
    var plantType = Plant.Type.TREE
    var photoUri: String = ""
    var taxonId: Long? = null
    var vernacularId: Long? = null

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

//    fun updateIsUsed(result: SearchResult) {
//        GlobalScope.launch(Dispatchers.IO) {
//            when {
//                result.hasTag(COMMON) -> vernacularRepo.setTagged(result.id, USED, result.hasTag(USED))
//                result.hasTag(SCIENTIFIC) -> taxonRepo.setTagged(result.id, USED, result.hasTag(USED))
//                else -> { }
//            }
//        }
//    }

    fun updateIsStarred(result: SearchResult) {
        GlobalScope.launch(Dispatchers.IO) {
            when {
                result.hasTag(COMMON) -> vernacularRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
                result.hasTag(SCIENTIFIC) -> taxonRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
            }
        }
    }
}
