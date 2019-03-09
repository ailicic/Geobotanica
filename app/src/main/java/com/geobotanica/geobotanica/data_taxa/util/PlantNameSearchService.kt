package com.geobotanica.geobotanica.data_taxa.util

import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag.*
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

// TODO: Consider moving all tags into a single table (except for COMMON/SCIENTIFIC, duh)
    // This will ease sorting based on tags and eliminate the need for mergeTagsOnIds

const val defaultPlantNameFilterFlags = 0b0

class PlantNameSearchService @Inject constructor (
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
) : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job


    class PlantNameSearch(
            val fun1: ( (String, Int) -> List<Long>? )? = null,
            val fun2: ( (String, String, Int) -> List<Long>? )? = null,
            tagList: List<PlantNameTag> = emptyList()
    ) {
        val tags: Int = tagList.fold(0) { acc, tag -> acc or tag.flag }

        val functionName: String
            get() {
                return (fun1 ?: fun2).toString()
                        .removePrefix("function ")
                        .removeSuffix(" (Kotlin reflection is not available)")
            }

        fun hasTag(tag: PlantNameTag) = tags and tag.flag != 0
    }

    private val singleWordSearchSequence = listOf(
        PlantNameSearch(fun1 = vernacularRepo::starredStartsWith, tagList = listOf(COMMON, STARRED)),
        PlantNameSearch(fun1 = taxonRepo::starredStartsWith, tagList = listOf(SCIENTIFIC, STARRED)),
        PlantNameSearch(fun1 = vernacularRepo::usedStartsWith, tagList = listOf(COMMON, USED)),
        PlantNameSearch(fun1 = taxonRepo::usedStartsWith, tagList = listOf(SCIENTIFIC, USED)),
        PlantNameSearch(fun1 = vernacularRepo::nonFirstWordStartsWith, tagList = listOf(COMMON)),
        PlantNameSearch(fun1 = vernacularRepo::firstWordStartsWith, tagList = listOf(COMMON)),
        PlantNameSearch(fun1 = taxonRepo::genericStartsWith, tagList = listOf(SCIENTIFIC)),
        PlantNameSearch(fun1 = taxonRepo::epithetStartsWith, tagList = listOf(SCIENTIFIC))
    )

    private val doubleWordSearchSequence = listOf(
        PlantNameSearch(fun2 = vernacularRepo::starredStartsWith, tagList = listOf(COMMON, STARRED)),
        PlantNameSearch(fun2 = taxonRepo::starredStartsWith, tagList = listOf(SCIENTIFIC, STARRED)),
        PlantNameSearch(fun2 = vernacularRepo::usedStartsWith, tagList = listOf(COMMON, USED)),
        PlantNameSearch(fun2 = taxonRepo::usedStartsWith, tagList = listOf(SCIENTIFIC, USED)),
        PlantNameSearch(fun2 = vernacularRepo::anyWordStartsWith, tagList = listOf(COMMON)),
        PlantNameSearch(fun2 = taxonRepo::genericOrEpithetStartsWith, tagList = listOf(SCIENTIFIC))
    )

    data class SearchResult(
        val id: Long, // Either vernacularId (COMMMON) or taxonId (SCIENTIFIC), depending on tag present
        var tags: Int, // Bitflags
        val plantName: String
    ) {
        fun hasTag(tag: PlantNameTag): Boolean = tags and tag.flag != 0
        fun toggleTag(tag: PlantNameTag) { tags = tags xor tag.flag }
        fun mergeTags(newTags: Int): SearchResult = apply { tags = tags or newTags }
        fun tagCount(): Int {
            var temp = tags
            var count = 0
            while (temp != 0) {
                if (temp and 0b1 != 0)
                    ++count
                temp = temp shr 1
            }
            return count
        }
    }

    enum class PlantNameTag(val flag: Int) {
        COMMON(     0b0000_0001),
        SCIENTIFIC( 0b0000_0010),
        STARRED(    0b0000_0100),
        USED(       0b0000_1000);
    }

    //    fun getDefault(searchFilters: PlantNameFilterOptions): List<SearchResult> {
//        return mutableListOf<SearchResult>().apply {
//            if (! searchFilters.isStarredFiltered) {
//                if (!searchFilters.isCommonFiltered)
//                    addAll(vernacularRepo.getAllStarred().map { mapIdToSearchResult(it, COMMON, isStarred = true) })
//                if (!searchFilters.isScientificFiltered)
//                    addAll(taxonRepo.getAllStarred().map { mapIdToSearchResult(it, SCIENTIFIC, isStarred = true) })
//                if (!searchFilters.isUsedFiltered)
//                    addAll(vernacularRepo.getAllUsed().map { mapIdToSearchResult(it, COMMON, isUsed = true) })
//                if (!searchFilters.isUsedFiltered)
//                    addAll(taxonRepo.getAllUsed().map { mapIdToSearchResult(it, SCIENTIFIC, isUsed = true) })
//            }
//        }
//    }


    // TODO: Implement this after merging the tag tables
//        fun getDefault(filterOptions: SearchFilterOptions): List<SearchResult> {
//        return mutableListOf<SearchResult>().apply {
            // Get all tagged vern / taxa
            // Filter
//            if (filterOptions.hasFilter()) {
//                if (!filterOptions.isCommonFiltered)
//                    addAll(vernacularRepo.getAllStarred().map { mapIdToSearchResult(it, COMMON, isStarred = true) })
//                if (!filterOptions.isScientificFiltered)
//                    addAll(taxonRepo.getAllStarred().map { mapIdToSearchResult(it, SCIENTIFIC, isStarred = true) })
//                if (!filterOptions.isUsedFiltered)
//                    addAll(vernacularRepo.getAllUsed().map { mapIdToSearchResult(it, COMMON, isUsed = true) })
//                if (!filterOptions.isUsedFiltered)
//                    addAll(taxonRepo.getAllUsed().map { mapIdToSearchResult(it, SCIENTIFIC, isUsed = true) })
//            }
//        }
//    }

    fun getDefault(filterOptions: SearchFilterOptions): List<SearchResult> = emptyList()

    @ExperimentalCoroutinesApi
    fun search(searchText: String, filterOptions: SearchFilterOptions): ReceiveChannel<List<SearchResult>> = produce {
        val words = searchText.split(' ').filter { it.isNotBlank() }
        val isSingleWord = words.size == 1
        Lg.d("Search words: $words")

        val aggregateResultIds = mutableListOf<Long>() // Used to remove duplicates
        val combinedResults = mutableListOf<SearchResult>()
        val searchSequence = if (isSingleWord) singleWordSearchSequence else doubleWordSearchSequence

        searchSequence.filter { filterOptions.shouldNotFilter(it) }.forEach forEachSearch@ { search ->
            if (combinedResults.size >= DEFAULT_RESULT_LIMIT)
                return@forEachSearch

            val time = measureTimeMillis {
                val limit = getLimit(combinedResults)
                val results =
                        if (isSingleWord) search.fun1!!(words[0], limit) ?: return@forEachSearch
                        else search.fun2!!(words[0], words[1], limit) ?: return@forEachSearch
                val uniqueIds = results subtract aggregateResultIds // Removes duplicates across multiple searches
                val mergeTagsOnIds = results intersect aggregateResultIds
                aggregateResultIds.addAll(results) // Keep record of all results for removing dupes

                combinedResults.addAll(uniqueIds.map { mapIdToSearchResult(it, search) })
                send(combinedResults
                        .map {
                            if (mergeTagsOnIds.contains(it.id)) {
                                it.mergeTags(search.tags)
                            } else
                                it
                        }
                        .filter { filterOptions.shouldNotFilter(it) }
                        .distinctBy { it.plantName }
                        .sortedByDescending { it.tagCount() }
                )
            }
            Lg.d("${search.functionName}: ${combinedResults.size} hits ($time ms)")
        }
        close()
    }

    private fun getLimit(filteredResults: List<SearchResult>) =
            DEFAULT_RESULT_LIMIT - filteredResults.size

    private fun mapIdToSearchResult(id: Long, search: PlantNameSearch): SearchResult {
        return SearchResult(id, search.tags, when {
            search.hasTag(COMMON) -> vernacularRepo.get(id)!!.vernacular!!.capitalize()
            search.hasTag(SCIENTIFIC) -> taxonRepo.get(id)!!.scientific.capitalize()
            else -> throw IllegalArgumentException("Must specify either COMMON or SCIENTIFIC tag")
        })
    }

    class SearchFilterOptions(val filterFlags: Int) {
        fun hasFilter(filterOption: PlantNameTag) = filterOption.flag and filterFlags != 0
        fun shouldNotFilter(search: PlantNameSearch) = (search.tags and (COMMON.flag or SCIENTIFIC.flag)) and filterFlags == 0
        fun shouldNotFilter(searchResult: SearchResult) = searchResult.tags and filterFlags == 0

        companion object {
            fun fromBooleans(
                    isCommonFiltered: Boolean,
                    isScientificFiltered: Boolean,
                    isStarredFiltered: Boolean,
                    isUsedFiltered: Boolean
            ): SearchFilterOptions {
                var filterFlags = 0b0
                if (isCommonFiltered)
                    filterFlags = filterFlags or COMMON.flag
                if (isScientificFiltered)
                    filterFlags = filterFlags or SCIENTIFIC.flag
                if (isStarredFiltered)
                    filterFlags = filterFlags or STARRED.flag
                if (isUsedFiltered)
                    filterFlags = filterFlags or USED.flag
                return SearchFilterOptions(filterFlags)
            }
        }
    }


}