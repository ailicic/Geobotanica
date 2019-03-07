package com.geobotanica.geobotanica.ui.newplantname

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameFilterOptions
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameType.*
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
    lateinit var plantNameFilterOptions: PlantNameFilterOptions
    val plantNameFilterFlags: Int
        get() = plantNameFilterOptions.filterFlags

    // TODO: Remove this init block (for testing)
    init {
        GlobalScope.launch(Dispatchers.IO) {
            Lg.d("Count vern: ${vernacularRepo.getCount()}")
            Lg.d("Count taxa: ${taxonRepo.getCount()}")
        }
    }

    @ExperimentalCoroutinesApi
    fun searchPlantName(string: String): ReceiveChannel<List<PlantNameSearchService.SearchResult>> =
        plantNameSearchService.search(string, plantNameFilterOptions)

    suspend fun getAllStarredPlantNames(): List<PlantNameSearchService.SearchResult> = withContext(Dispatchers.IO) {
        plantNameSearchService.getAllStarred(plantNameFilterOptions)
    }

    fun setStarred(plantNameType: PlantNameSearchService.PlantNameType, id: Long, isStarred: Boolean) {
        GlobalScope.launch(Dispatchers.IO) {
            when (plantNameType) {
                SCIENTIFIC -> taxonRepo.setStarred(id, isStarred)
                VERNACULAR -> vernacularRepo.setStarred(id, isStarred)
            }
        }
    }
}
