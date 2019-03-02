package com.geobotanica.geobotanica.ui.newplantname

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data_ro.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_ro.repo.TaxonRepo
import com.geobotanica.geobotanica.data_ro.repo.VernacularRepo
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
    fun searchPlantName(string: String): ReceiveChannel<List<PlantNameSearchService.SearchResult>> =
        plantNameSearchService.search(string)
}

// TODO: Move to separate file
class PlantNameSearchService @Inject constructor (
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
) : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    enum class PlantNameType { SCIENTIFIC, VERNACULAR }

    data class SearchResult(val plantNameType: PlantNameType, val id: Long, val name: String)

    private data class SingleWordSearch(
            val function: (String, Int) -> Set<Long>?,
            val resultType: PlantNameType
    ) {
        val functionName: String
            get() {
                return function.toString()
                    .removePrefix("function ")
                    .removeSuffix(" (Kotlin reflection is not available)")
            }
    }

    private data class DoubleWordSearch(
            val function: (String, String, Int) -> Set<Long>?,
            val resultType: PlantNameType
    ) {
        val functionName: String
            get() {
                return function.toString()
                    .removePrefix("function ")
                    .removeSuffix(" (Kotlin reflection is not available)")
            }
    }

    private val singleWordSearchSequence = listOf(
            SingleWordSearch(vernacularRepo::nonFirstWordStartsWith, PlantNameType.VERNACULAR),
            SingleWordSearch(vernacularRepo::firstWordStartsWith, PlantNameType.VERNACULAR),
            SingleWordSearch(taxonRepo::genericStartsWith, PlantNameType.SCIENTIFIC),
            SingleWordSearch(taxonRepo::epithetStartsWith, PlantNameType.SCIENTIFIC)
    )

    private val doubleWordSearchSequence = listOf(
            DoubleWordSearch(vernacularRepo::anyWordStartsWith, PlantNameType.VERNACULAR),
            DoubleWordSearch(taxonRepo::genericOrEpithetStartsWith, PlantNameType.SCIENTIFIC)
    )

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

    private suspend fun ProducerScope<List<SearchResult>>.performSingleWordSearch(word: String) {
        val combinedResults = mutableListOf<SearchResult>() // Is ordered by wordSearchSequence
        singleWordSearchSequence.forEach forEachSearch@{ search ->
            if (combinedResults.size >= DEFAULT_RESULT_LIMIT)
                return@forEachSearch

            val time = measureTimeMillis {
                val results = search.function(word, getLimit(combinedResults)) ?: return@forEachSearch

                combinedResults.addAll(
                    results.map { id: Long -> mapIdToSearchResult(search.resultType, id) }
                )
            }
            Lg.d("${search.functionName}: ${combinedResults.size} hits ($time ms)")
            send(combinedResults) // Send partial results as they become available
        }
    }

    private suspend fun ProducerScope<List<SearchResult>>.performDoubleWordSearch(first: String, second: String) {
        val combinedResults = mutableListOf<SearchResult>() // Is ordered by wordSearchSequence
        doubleWordSearchSequence.forEach forEachSearch@{ search ->
            if (combinedResults.size >= DEFAULT_RESULT_LIMIT)
                return@forEachSearch

            val time = measureTimeMillis {
                val results = search.function(first, second, getLimit(combinedResults)) ?: return@forEachSearch

                combinedResults.addAll(
                    results.map { id: Long -> mapIdToSearchResult(search.resultType, id) }
                )
            }
            Lg.d("${search.functionName}: ${combinedResults.size} hits ($time ms)")
            send(combinedResults) // Send partial results as they become available
        }
    }

    private fun getLimit(combinedResults: MutableList<PlantNameSearchService.SearchResult>) =
            DEFAULT_RESULT_LIMIT - combinedResults.size

    private fun mapIdToSearchResult(resultType: PlantNameType, result: Long): SearchResult {
        return SearchResult(resultType, result, when(resultType) {
            PlantNameType.VERNACULAR -> vernacularRepo.get(result)!!.vernacular!!
            PlantNameType.SCIENTIFIC -> taxonRepo.get(result)!!.latinName
        })
    }
}