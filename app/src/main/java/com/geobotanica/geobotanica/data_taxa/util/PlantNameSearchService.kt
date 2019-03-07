package com.geobotanica.geobotanica.data_taxa.util

import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameFilterOptions.Flags.*
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

const val defaultPlantNameFilterFlags = 0b0

class PlantNameSearchService @Inject constructor (
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
) : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    enum class PlantNameType { SCIENTIFIC, VERNACULAR }

    data class PlantNameSearch(
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

    fun getAllStarred(plantNameFilterOptions: PlantNameFilterOptions): List<SearchResult> {
        return mutableListOf<SearchResult>().apply {
            if (! plantNameFilterOptions.isStarredFiltered) {
                if (!plantNameFilterOptions.isVernacularFiltered)
                    addAll(vernacularRepo.getAllStarred().map { mapIdToSearchResult(it, VERNACULAR, isStarred = true) })
                if (!plantNameFilterOptions.isScientificFiltered)
                    addAll(taxonRepo.getAllStarred().map { mapIdToSearchResult(it, SCIENTIFIC, isStarred = true) })
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun search(searchText: String, plantNameFilterOptions: PlantNameFilterOptions): ReceiveChannel<List<SearchResult>> = produce {
        val words = searchText.split(' ').filter { it.isNotBlank() }
        val isSingleWord = words.size == 1
        Lg.d("Search words: $words")

        val starredDeduplicator = StarredDeduplicator()
        val combinedResults = mutableListOf<SearchResult>() // Is ordered by searchSequence
        val searchSequence = if (isSingleWord) singleWordSearchSequence else doubleWordSearchSequence

        searchSequence
                .filterSearches(plantNameFilterOptions)
                .forEach forEachSearch@ { search ->
            if (combinedResults.size >= DEFAULT_RESULT_LIMIT)
                return@forEachSearch

            val time = measureTimeMillis {
                val limit = getLimit(combinedResults)
                var results =
                        if (isSingleWord) search.fun1!!(words[0], limit) ?: return@forEachSearch
                        else search.fun2!!(words[0], words[1], limit) ?: return@forEachSearch
                results = starredDeduplicator.process(results, search.isStarred, search.plantNameType)
                if (search.isStarred && plantNameFilterOptions.isStarredFiltered)
                    return@forEachSearch
                combinedResults.addAll(
                        results.map { id: Long -> mapIdToSearchResult(id, search.plantNameType, search.isStarred) }
                )
            }
            Lg.d("${search.functionName}: ${combinedResults.size} hits ($time ms)")
            send(combinedResults.distinctBy { it.name }) // Send partial results as they become available
        }
        close()
    }

    private fun getLimit(combinedResults: MutableList<SearchResult>) =
            DEFAULT_RESULT_LIMIT - combinedResults.size

    private fun mapIdToSearchResult(id: Long, plantNameType: PlantNameType, isStarred: Boolean): SearchResult {
        return SearchResult(id, plantNameType, isStarred, when(plantNameType) {
            VERNACULAR -> vernacularRepo.get(id)!!.vernacular!!.capitalize()
            SCIENTIFIC -> taxonRepo.get(id)!!.scientific.capitalize()
        })
    }

    private fun List<PlantNameSearch>.filterSearches(
        plantNameFilterOptions: PlantNameFilterOptions
    ): List<PlantNameSearch> {
        var filteredSearches = this
        if (plantNameFilterOptions.isVernacularFiltered)
            filteredSearches = filteredSearches.filter { it.plantNameType != VERNACULAR }
        if (plantNameFilterOptions.isScientificFiltered)
            filteredSearches = filteredSearches.filter { it.plantNameType != SCIENTIFIC }
        return filteredSearches
    }

    class PlantNameFilterOptions(
            val isVernacularFiltered: Boolean = false,
            val isScientificFiltered: Boolean = false,
            val isStarredFiltered: Boolean = false,
            val isHistoryFiltered: Boolean = false
    ) {
        val filterFlags: Int
            get() {
                var flags = 0
                if (isVernacularFiltered)      flags = flags or FILTER_VERNACULAR.flag
                if (isScientificFiltered)  flags = flags or FILTER_SCIENTIFIC.flag
                if (isStarredFiltered)     flags = flags or FILTER_STARRED.flag
                if (isHistoryFiltered)     flags = flags or FILTER_HISTORY.flag
                return flags
            }

        constructor(flags: Int): this(
                isVernacularFiltered =      flags and FILTER_VERNACULAR.flag != 0,
                isScientificFiltered =  flags and FILTER_SCIENTIFIC.flag != 0,
                isStarredFiltered =     flags and FILTER_STARRED.flag != 0,
                isHistoryFiltered =     flags and FILTER_HISTORY.flag != 0
        )

        enum class Flags(val flag: Int) {
            FILTER_VERNACULAR(  0b00000001),
            FILTER_SCIENTIFIC(  0b00000010),
            FILTER_STARRED(     0b00000100),
            FILTER_HISTORY(     0b00001000);
        }
    }

    class StarredDeduplicator {
        private val starredTaxaIds = mutableListOf<Long>()
        private val starredVernacularIds = mutableListOf<Long>()

        fun process(results: List<Long>, isStarred: Boolean, plantNameType: PlantNameType): List<Long> {
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