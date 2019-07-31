package com.geobotanica.geobotanica.ui.searchplantname

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchFilterOptions
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
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

    var searchText = ""
    lateinit var searchFilterOptions: SearchFilterOptions

    // TODO: Remove this init block (for testing)
    init {
        viewModelScope.launch(Dispatchers.IO) {
            Lg.d("Count vern: ${vernacularRepo.getCount()}")
            Lg.d("Count taxa: ${taxonRepo.getCount()}")
            Lg.d("Count vernType: ${vernacularRepo.getTypeCount()}")
            Lg.d("Count taxaType: ${taxonRepo.getTypeCount()}")

//            var i = 0
//            var countZero = 0
//            var countOne = 0
//            var countTwo = 0
//            var countThree = 0
//            var countFour = 0
//
//            Lg.d("STARTING NOW")
//            val cursor = taxonRepo.getAllIds()
//            cursor.moveToFirst()
//            do {
//                ++i
//                if (i % 10_000 == 0)
//                    Lg.d("${i / 10_000}%")
//                val type = taxonRepo.getTypes(cursor.getLong(0))
////                Lg.d("Cursor at ${cursor.getLong(0)}")
//                when (type.size) {
//                    0 -> ++ countZero
//                    1 -> ++ countOne
//                    2 -> ++ countTwo
//                    3 -> ++ countThree
//                    4 -> ++ countFour
//                }
//            } while (cursor.moveToNext())
//
//            Lg.d("countZero=$countZero")
//            Lg.d("countOne=$countOne")
//            Lg.d("countTwo=$countTwo")
//            Lg.d("countThree=$countThree")
//            Lg.d("countFour=$countFour")
//            Lg.d("total=${countZero + countOne + countTwo + countThree + countFour}")
        }
    }

    @ExperimentalCoroutinesApi
    fun searchPlantName(string: String): ReceiveChannel<List<SearchResult>> =
        plantNameSearchService.search(string, searchFilterOptions)

    fun updateIsStarred(result: SearchResult) = viewModelScope.launch {
        when {
            result.hasTag(COMMON) -> vernacularRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
            result.hasTag(SCIENTIFIC) -> taxonRepo.setTagged(result.id, STARRED, result.hasTag(STARRED))
        }
    }

    fun updateStarredTimestamp(result: SearchResult) = viewModelScope.launch {
        when {
            result.hasTag(COMMON) -> vernacularRepo.updateTagTimestamp(result.id, STARRED)
            result.hasTag(SCIENTIFIC) -> taxonRepo.updateTagTimestamp(result.id, STARRED)
        }
    }
}
