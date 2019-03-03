package com.geobotanica.geobotanica.data_taxa.util

import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameType.SCIENTIFIC
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameType.VERNACULAR
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis


class PlantNameSearchService @Inject constructor (
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
) : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    enum class PlantNameType { SCIENTIFIC, VERNACULAR }

    private data class PlantNameSearch(
            val plantNameType: PlantNameType,
            val fun1: ( (String, Int) -> List<Long>? )? = null,
            val fun2: ( (String, String, Int) -> List<Long>? )? = null,
            val isStarred: Boolean = false
    ) {
        val functionName: String
            get() {
                return (fun1 ?: fun2).toString()
                    .removePrefix("function ")
                    .removeSuffix(" (Kotlin reflection is not available)")
            }
    }

    private val singleWordSearchSequence = listOf(
        PlantNameSearch(VERNACULAR, fun1 = vernacularRepo::starredStartsWith, isStarred = true),
        PlantNameSearch(SCIENTIFIC, fun1 = taxonRepo::starredStartsWith, isStarred = true),
        PlantNameSearch(VERNACULAR, fun1 = vernacularRepo::nonFirstWordStartsWith),
        PlantNameSearch(VERNACULAR, fun1 = vernacularRepo::firstWordStartsWith),
        PlantNameSearch(SCIENTIFIC, fun1 = taxonRepo::genericStartsWith),
        PlantNameSearch(SCIENTIFIC, fun1 = taxonRepo::epithetStartsWith)
    )

    private val doubleWordSearchSequence = listOf(
        PlantNameSearch(VERNACULAR, fun2 = vernacularRepo::starredStartsWith, isStarred = true),
        PlantNameSearch(SCIENTIFIC, fun2 = taxonRepo::starredStartsWith, isStarred = true),
        PlantNameSearch(VERNACULAR, fun2 = vernacularRepo::anyWordStartsWith),
        PlantNameSearch(SCIENTIFIC, fun2 = taxonRepo::genericOrEpithetStartsWith)
    )

    data class SearchResult(
        val id: Long,
        val plantNameType: PlantNameType,
        var isStarred: Boolean,
        val name: String
    )

    fun getAllStarred(): List<SearchResult> {
        return mutableListOf<SearchResult>().apply {
            addAll(taxonRepo.getAllStarred().map { mapIdToSearchResult(it, SCIENTIFIC, isStarred = true) })
            addAll(vernacularRepo.getAllStarred().map { mapIdToSearchResult(it, VERNACULAR, isStarred = true) })
        }
    }

    @ExperimentalCoroutinesApi
    fun search(searchText: String): ReceiveChannel<List<SearchResult>> = produce {
        val words = searchText.split(' ').filter { it.isNotBlank() }
        val isSingleWord = words.size == 1
        Lg.d("Search words: $words")

        val starredFilter = StarredFilter()
        val combinedResults = mutableListOf<SearchResult>() // Is ordered by wordSearchSequence
        val searchSequence = if (isSingleWord) singleWordSearchSequence else doubleWordSearchSequence
        searchSequence.forEach forEachSearch@{ search ->
            if (combinedResults.size >= DEFAULT_RESULT_LIMIT)
                return@forEachSearch

            val time = measureTimeMillis {
                val limit = getLimit(combinedResults)
                var results =
                        if (isSingleWord) search.fun1!!(words[0], limit) ?: return@forEachSearch
                        else search.fun2!!(words[0], words[1], limit) ?: return@forEachSearch
                results = starredFilter.filter(results, search.isStarred, search.plantNameType)
                combinedResults.addAll(
                        results.map { id: Long -> mapIdToSearchResult(id, search.plantNameType, search.isStarred) }
                                .distinctBy { it.name }
                )
            }
            Lg.d("${search.functionName}: ${combinedResults.size} hits ($time ms)")
            send(combinedResults) // Send partial results as they become available
        }
        close()
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