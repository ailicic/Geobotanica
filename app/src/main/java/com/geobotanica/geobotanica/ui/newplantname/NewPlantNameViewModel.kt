package com.geobotanica.geobotanica.ui.newplantname

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.ui.newplantname.PlantNameSearchService.PlantNameType
import com.geobotanica.geobotanica.ui.newplantname.PlantNameSearchService.PlantNameType.SCIENTIFIC
import com.geobotanica.geobotanica.ui.newplantname.PlantNameSearchService.PlantNameType.VERNACULAR
import com.geobotanica.geobotanica.ui.newplantname.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

@Singleton
class NewPlantNameViewModel @Inject constructor (
//        private val taxonRepo: TaxonRepo,
//        private val vernacularRepo: VernacularRepo
        val taxonRepo: TaxonRepo,    // TODO: Make private (helps verify db pre-pop now) -> AFTER DOWNLOAD SCREEN IS CREATED
        val vernacularRepo: VernacularRepo
): ViewModel() {
    var userId = 0L
    var plantType = Plant.Type.TREE
    var photoUri: String = ""
    var commonName: String? = null
    var latinName: String? = null

    // TODO: Inject this -> AFTER DOWNLOAD SCREEN IS CREATED
    private val plantNameSearchService = PlantNameSearchService(taxonRepo, vernacularRepo)

    @ExperimentalCoroutinesApi
    fun searchPlantName(string: String): ReceiveChannel<List<SearchResult>> =
        plantNameSearchService.search(string)

    suspend fun getAllStarredPlantNames(): List<SearchResult> = withContext(Dispatchers.IO) {
        plantNameSearchService.getAllStarred()
    }

    fun setStarred(plantNameType: PlantNameType, id: Long, isStarred: Boolean) {
        GlobalScope.launch(Dispatchers.IO) {
            when (plantNameType) {
                SCIENTIFIC -> taxonRepo.setStarred(id, isStarred)
                VERNACULAR -> vernacularRepo.setStarred(id, isStarred)
            }
        }
    }
}

// TODO: Eliminate duplicate vernaculars (not an issue for taxa)
// TODO: Figure out how to merge single/double word searches
// TODO: Move to separate file
class PlantNameSearchService @Inject constructor (
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
) : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    enum class PlantNameType { SCIENTIFIC, VERNACULAR }

    private data class SingleWordSearch(
            val function: (String, Int) -> List<Long>?,
            val plantNameType: PlantNameType,
            val isStarred: Boolean = false
    ) {
        val functionName: String
            get() {
                return function.toString()
                    .removePrefix("function ")
                    .removeSuffix(" (Kotlin reflection is not available)")
            }
    }

    private data class DoubleWordSearch(
            val function: (String, String, Int) -> List<Long>?,
            val plantNameType: PlantNameType,
            val isStarred: Boolean = false
    ) {
        val functionName: String
            get() {
                return function.toString()
                    .removePrefix("function ")
                    .removeSuffix(" (Kotlin reflection is not available)")
            }
    }

    private val singleWordSearchSequence = listOf(
            SingleWordSearch(vernacularRepo::starredStartsWith, VERNACULAR, isStarred = true),
            SingleWordSearch(taxonRepo::starredStartsWith, SCIENTIFIC, isStarred = true),
            SingleWordSearch(vernacularRepo::nonFirstWordStartsWith, VERNACULAR),
            SingleWordSearch(vernacularRepo::firstWordStartsWith, VERNACULAR),
            SingleWordSearch(taxonRepo::genericStartsWith, SCIENTIFIC),
            SingleWordSearch(taxonRepo::epithetStartsWith, SCIENTIFIC)
    )

    private val doubleWordSearchSequence = listOf(
            DoubleWordSearch(vernacularRepo::starredStartsWith, VERNACULAR, isStarred = true),
            DoubleWordSearch(taxonRepo::starredStartsWith, SCIENTIFIC, isStarred = true),
            DoubleWordSearch(vernacularRepo::anyWordStartsWith, VERNACULAR),
            DoubleWordSearch(taxonRepo::genericOrEpithetStartsWith, SCIENTIFIC)
    )

    data class SearchResult(
            val id: Long,
            val plantNameType: PlantNameType,
            var isStarred: Boolean,
            val name: String
    )

    fun getAllStarred(): List<SearchResult> {
        val starred = mutableListOf<SearchResult>()
        starred.addAll(taxonRepo.getAllStarred().map {
            mapIdToSearchResult(it, SCIENTIFIC, isStarred = true)
        })
        starred.addAll(vernacularRepo.getAllStarred().map {
            mapIdToSearchResult(it, VERNACULAR, isStarred = true)
        })
        return starred
    }

    @ExperimentalCoroutinesApi
    fun search(searchText: String): ReceiveChannel<List<SearchResult>> = produce {
        if (searchText.isEmpty()) {
            send(emptyList())
            close()
            return@produce
        }

        val words = searchText.split(' ').filter { it.isNotBlank() }
        Lg.d("Search words: $words")

        when {
            words.size == 1 -> performSingleWordSearch(words[0])
            words.size > 1 -> performDoubleWordSearch(words[0], words[1])
        }
        close()
    }

    @ExperimentalCoroutinesApi
    private suspend fun ProducerScope<List<SearchResult>>.performSingleWordSearch(word: String) {
        val starredFilter = StarredFilter()
        val combinedResults = mutableListOf<SearchResult>() // Is ordered by wordSearchSequence
        singleWordSearchSequence.forEach forEachSearch@{ search ->
            if (combinedResults.size >= DEFAULT_RESULT_LIMIT)
                return@forEachSearch

            val time = measureTimeMillis {
                var results = search.function(word, getLimit(combinedResults)) ?: return@forEachSearch
                results = starredFilter.filter(results, search.isStarred, search.plantNameType)
                combinedResults.addAll(
                    results.map { id: Long -> mapIdToSearchResult(id, search.plantNameType, search.isStarred) }
                )
            }
            Lg.d("${search.functionName}: ${combinedResults.size} hits ($time ms)")
            send(combinedResults) // Send partial results as they become available
        }
    }

    @ExperimentalCoroutinesApi
    private suspend fun ProducerScope<List<SearchResult>>.performDoubleWordSearch(first: String, second: String) {
        val starredFilter = StarredFilter()
        val combinedResults = mutableListOf<SearchResult>() // Is ordered by wordSearchSequence
        doubleWordSearchSequence.forEach forEachSearch@{ search ->
            if (combinedResults.size >= DEFAULT_RESULT_LIMIT)
                return@forEachSearch

            val time = measureTimeMillis {
                var results = search.function(first, second, getLimit(combinedResults)) ?: return@forEachSearch
                results = starredFilter.filter(results, search.isStarred, search.plantNameType)

                combinedResults.addAll(
                    results.map { id: Long ->
                        mapIdToSearchResult(id, search.plantNameType, isStarred = search.isStarred)
                    }
                )
            }
            Lg.d("${search.functionName}: ${combinedResults.size} hits ($time ms)")
            send(combinedResults) // Send partial results as they become available
        }
    }

    private fun getLimit(combinedResults: MutableList<SearchResult>) =
            DEFAULT_RESULT_LIMIT - combinedResults.size

    private fun mapIdToSearchResult(id: Long, plantNameType: PlantNameType, isStarred: Boolean): SearchResult {
        return SearchResult(id, plantNameType, isStarred, when(plantNameType) {
            VERNACULAR -> vernacularRepo.get(id)!!.vernacular!!.capitalize()
            SCIENTIFIC -> taxonRepo.get(id)!!.latinName.capitalize()
        })
    }


    class StarredFilter {
        private val starredTaxaIds = mutableListOf<Long>()
        private val starredVernacularIds = mutableListOf<Long>()

        fun filter(results: List<Long>, isStarred: Boolean, plantNameType: PlantNameType): List<Long> {
            if (isStarred) {
                when (plantNameType) {
                    SCIENTIFIC -> starredTaxaIds.addAll(results)
                    VERNACULAR -> starredVernacularIds.addAll(results)
                }
            } else {
                return when (plantNameType) {
                    SCIENTIFIC -> results.filterNot { starredTaxaIds.contains(it) }
                    VERNACULAR -> results.filterNot { starredVernacularIds.contains(it) }
                }
            }
            return results
        }
    }
}