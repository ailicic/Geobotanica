package com.geobotanica.geobotanica.data_taxa.util

import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

/*
    NOTES
    - The current search strategy produces results as fast as possible at the expense of complexity (good for dynamic searching)
    - The search implementation would be simpler (but produce results slower) if the full search was performed first,
         then tags were applied after (via tagged id search, then intersect with results)
 */

const val defaultFilterFlags = 0b0

class PlantNameSearchService @Inject constructor (
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
) : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    private val defaultSearchSequence = listOf(
        PlantNameSearch(fun0 = vernacularRepo::getAllStarred, tagList = listOf(COMMON, STARRED)),
        PlantNameSearch(fun0 = taxonRepo::getAllStarred, tagList = listOf(SCIENTIFIC, STARRED)),
        PlantNameSearch(fun0 = vernacularRepo::getAllUsed, tagList = listOf(COMMON, USED)),
        PlantNameSearch(fun0 = taxonRepo::getAllUsed, tagList = listOf(SCIENTIFIC, USED))
    )

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

    private val suggestedNameSearchSequence = listOf(
        PlantNameSearch(fun0 = vernacularRepo::starredFromTaxonId, tagList = listOf(COMMON, STARRED)),
        PlantNameSearch(fun0 = vernacularRepo::usedFromTaxonId, tagList = listOf(COMMON, USED)),
        PlantNameSearch(fun0 = vernacularRepo::fromTaxonId, tagList = listOf(COMMON)),

        PlantNameSearch(fun0 = taxonRepo::starredFromVernacularId, tagList = listOf(SCIENTIFIC, STARRED)),
        PlantNameSearch(fun0 = taxonRepo::usedFromVernacularId, tagList = listOf(SCIENTIFIC, USED)),
        PlantNameSearch(fun0 = taxonRepo::fromVernacularId, tagList = listOf(SCIENTIFIC))
    )

    @Suppress("EXPERIMENTAL_API_USAGE") // For flowOn(Dispatchers.Default)
    fun search(
        searchText: String,
        filterOptions: SearchFilterOptions = SearchFilterOptions(),
        isSuggestionsSearch: Boolean = false // If true, Taxon/Vernacular id is passed in searchText
    ): Flow<List<SearchResult>> = flow {
        val words = searchText.split(' ').filter { it.isNotBlank() }
        val wordCount = words.size
        Lg.d("Search words: $words")

        val aggregateResultIds = mutableListOf<Long>()
        val aggregateResults = mutableListOf<SearchResult>()
        val searchSequence = getSearchSequence(wordCount, isSuggestionsSearch)

        searchSequence.filter { filterOptions.shouldNotFilter(it) }.forEach forEachSearch@ { search ->
            if (aggregateResults.size >= DEFAULT_RESULT_LIMIT)
                return@forEachSearch

            val time = measureTimeMillis {
                val results = getSearchResults(words, search, getLimit(aggregateResults), isSuggestionsSearch)
                        ?: return@forEachSearch
                val uniqueIds = results subtract aggregateResultIds
                val mergeTagsOnIds = results intersect aggregateResultIds

                aggregateResultIds.addAll(results)
                aggregateResults.addAll(uniqueIds.map { mapIdToSearchResult(it, search) })

                emit(aggregateResults
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
            Lg.d("${search.functionName}: ${aggregateResults.size} hits ($time ms)")
        }
    }.flowOn(Dispatchers.Default)

    fun searchSuggestedCommonNames(taxonId: Long): Flow<List<SearchResult>> =
        search(taxonId.toString(), SearchFilterOptions(SCIENTIFIC.flag), true)


    fun searchSuggestedScientificNames(vernacularId: Long): Flow<List<SearchResult>> =
        search(vernacularId.toString(), SearchFilterOptions(COMMON.flag), true)

    private fun getLimit(filteredResults: List<SearchResult>) =
            DEFAULT_RESULT_LIMIT - filteredResults.size

    private fun getSearchSequence(wordCount: Int, isSuggestionsSearch: Boolean): List<PlantNameSearch> {
        return if (isSuggestionsSearch)
            suggestedNameSearchSequence
        else when (wordCount) {
            0 -> defaultSearchSequence
            1 -> singleWordSearchSequence
            else -> doubleWordSearchSequence
        }
    }

    // TODO: Why is fun0/fun1/fun2 not detected as suspending?
    @Suppress("RedundantSuspendModifier")
    private suspend fun getSearchResults(
            words: List<String>,
            search: PlantNameSearch,
            limit: Int,
            isSuggestionsSearch: Boolean): List<Long>? {
        return if (isSuggestionsSearch)
            search.fun0!!(words[0].toInt())
        else when (words.size) {
            0 -> search.fun0!!(limit)
            1 -> search.fun1!!(words[0], limit)
            else -> search.fun2!!(words[0], words[1], limit)
        }
    }

    private suspend fun mapIdToSearchResult(id: Long, search: PlantNameSearch): SearchResult {
        val plantName: String = when {
            search.hasTag(COMMON) -> vernacularRepo.get(id)!!.vernacular!!.capitalize()
            search.hasTag(SCIENTIFIC) -> taxonRepo.get(id)!!.scientific.capitalize()
            else -> throw IllegalArgumentException("Must specify either COMMON or SCIENTIFIC tag")
        }
        val plantTypes: Int = when {
            search.hasTag(COMMON) -> vernacularRepo.getTypes(id)
            search.hasTag(SCIENTIFIC) -> taxonRepo.getTypes(id)
            else -> throw IllegalArgumentException("Must specify either COMMON or SCIENTIFIC tag")
        }
        return SearchResult(id, search.tags, plantTypes, plantName)
    }

    data class PlantNameSearch(
            val fun0: ( suspend (Int) -> List<Long>? )? = null,
            val fun1: ( suspend (String, Int) -> List<Long>? )? = null,
            val fun2: ( suspend (String, String, Int) -> List<Long>? )? = null,
            val tagList: List<PlantNameTag> = emptyList()
    ) {
        val tags: Int = tagList.fold(0) { acc, tag -> acc or tag.flag }

        val functionName: String
            get() {
                return (fun0 ?: fun1 ?: fun2).toString()
                    .removePrefix("function ")
                    .removeSuffix(" (Kotlin reflection is not available)")
            }

        fun hasTag(tag: PlantNameTag) = tags and tag.flag != 0
    }

    data class SearchResult(
        val id: Long, // Either vernacularId (COMMMON) or taxonId (SCIENTIFIC), depending on tag present
        var tags: Int, // Bitflags
        val plantTypes: Int, // Bitflags
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

    data class SearchFilterOptions(val filterFlags: Int = defaultFilterFlags) {
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