package com.geobotanica.geobotanica.data_taxa

import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.entity.Taxon
import com.geobotanica.geobotanica.data_taxa.entity.Vernacular
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchFilterOptions
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.util.SpekExt.beforeEachFlowTest
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.collect
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SearchPlantNameServiceTest : Spek({

    val taxonRepo = mockk<TaxonRepo>()
    val vernacularRepo = mockk<VernacularRepo>()

    val plantNameSearchService by memoized { PlantNameSearchService(taxonRepo, vernacularRepo) }

    val vBag = VernacularBag()
    val tBag = TaxonBag()

    val collected by memoized { mutableListOf<List<SearchResult>>() }

    beforeEachTest {
        clearMocks(taxonRepo, vernacularRepo)
    }

    describe("Search") {
        beforeEachTest {
            coEvery { vernacularRepo.getTypes(any()) } returns 0
            coEvery { taxonRepo.getTypes(any()) } returns 0

            val slot = CapturingSlot<Long>()
            coEvery { vernacularRepo.get(capture(slot)) } answers {
                val captured = slot.captured
                vBag.get(captured)
            }
            coEvery { taxonRepo.get(capture(slot)) } answers {
                val captured = slot.captured
                tBag.get(captured)
            }
        }

        context("Empty db") {
            beforeEachTest {
                coEvery { vernacularRepo.getAllStarred(any()) } returns emptyList()
                coEvery { vernacularRepo.getAllUsed(any()) } returns emptyList()
                coEvery { taxonRepo.getAllStarred(any()) } returns emptyList()
                coEvery { taxonRepo.getAllUsed(any()) } returns emptyList()
            }

            context("Default search sequence") {
                beforeEachFlowTest {
                    plantNameSearchService.search("").collect { collected.add(it) }
                }

                it("Should return nothing") {
                    collected.forEach { it shouldEqual emptyList() }
                }
            }
        }

        context("Populated db") {

            context("Default search") {
                val vStarred = vBag.getSearchResult("starred", STARRED)
                val vUsed = vBag.getSearchResult("used", USED)
                val vStarredUsed = vBag.getSearchResult("starredUsed", STARRED, USED)

                val tStarred = tBag.getSearchResult("starred", STARRED)
                val tUsed = tBag.getSearchResult("used", USED)
                val tStarredUsed = tBag.getSearchResult("starredUsed", STARRED, USED)

                beforeEachTest {
                    coEvery { vernacularRepo.getAllStarred(any()) } returns vBag.getIdsOf("starred", "starredUsed")
                    coEvery { vernacularRepo.getAllUsed(any()) } returns vBag.getIdsOf("used", "starredUsed")
                    coEvery { taxonRepo.getAllStarred(any()) } returns tBag.getIdsOf("starred", "starredUsed")
                    coEvery { taxonRepo.getAllUsed(any()) } returns tBag.getIdsOf("used", "starredUsed")
                }

                context("No search filter") {
                    beforeEachFlowTest {
                        plantNameSearchService.search("").collect { collected.add(it) }
                    }
                    it("1st emission correct") {
                        collected[0] shouldEqual listOf(vStarred, vStarredUsed)
                    }
                    it("2nd emission correct") {
                        collected[1] shouldEqual listOf(vStarred, vStarredUsed, tStarred, tStarredUsed)
                    }
                    it("3rd emission correct") {
                        collected[2] shouldEqual listOf(vStarredUsed, vStarred, tStarred, tStarredUsed, vUsed)
                    }
                    it("4th emission correct") {
                        collected[3] shouldEqual listOf(vStarredUsed, tStarredUsed, vStarred, tStarred, vUsed, tUsed)
                    }
                }

                context("Starred filtered out") {
                    beforeEachFlowTest {
                        plantNameSearchService.search("", SearchFilterOptions(STARRED.flag))
                                .collect { collected.add(it) }
                    }

                    it("Should skip starred searches") { collected.size shouldEqual 2 }
                    it("1st emission correct") {
                        collected[0] shouldEqual listOf(vUsed, vStarredUsed.withoutTag(STARRED))
                    }
                    it("2nd emission correct") {
                        collected[1] shouldEqual listOf(vUsed, vStarredUsed.withoutTag(STARRED), tUsed, tStarredUsed.withoutTag(STARRED))
                    }
                }

                context("Used filtered out") {
                    beforeEachFlowTest {
                        plantNameSearchService.search("", SearchFilterOptions(USED.flag))
                                .collect { collected.add(it) }
                    }

                    it("Should skip used searches") { collected.size shouldEqual 2 }
                    it("1st emission correct") {
                        collected[0] shouldEqual listOf(vStarred, vStarredUsed.withoutTag(USED))
                    }
                    it("2nd emission correct") {
                        collected[1] shouldEqual listOf(vStarred, vStarredUsed.withoutTag(USED), tStarred, tStarredUsed.withoutTag(USED))
                    }
                }

                context("Vernaculars filtered out") {
                    beforeEachFlowTest {
                        plantNameSearchService.search("", SearchFilterOptions(COMMON.flag))
                                .collect { collected.add(it) }
                    }

                    it("Should skip vernacular searches") { collected.size shouldEqual 2 }
                    it("1st emission correct") { collected[0] shouldEqual listOf(tStarred, tStarredUsed) }
                    it("2nd emission correct") { collected[1] shouldEqual listOf(tStarredUsed, tStarred, tUsed) }
                }

                context("Taxa filtered out") {
                    beforeEachFlowTest {
                        plantNameSearchService.search("", SearchFilterOptions(SCIENTIFIC.flag))
                                .collect { collected.add(it) }
                    }

                    it("Should skip taxa searches") { collected.size shouldEqual 2 }
                    it("1st emission correct") { collected[0] shouldEqual listOf(vStarred, vStarredUsed) }
                    it("2nd emission correct") { collected[1] shouldEqual listOf(vStarredUsed, vStarred, vUsed) }
                }

                context("Starred and taxa filtered out") {
                    beforeEachFlowTest {
                        plantNameSearchService.search(
                                "", SearchFilterOptions(STARRED.flag or SCIENTIFIC.flag)
                        ).collect { collected.add(it) }
                    }

                    it("Should skip starred/taxa searches") { collected.size shouldEqual 1 }
                    it("1st emission correct") { collected[0] shouldEqual listOf(vUsed, vStarredUsed.withoutTag(STARRED)) }
                }

                context("Used and vernaculars filtered out") {
                    beforeEachFlowTest {
                        plantNameSearchService.search(
                                "", SearchFilterOptions(USED.flag or COMMON.flag)
                        ).collect { collected.add(it) }
                    }

                    it("Should skip used/vernacular searches") { collected.size shouldEqual 1 }
                    it("1st emission correct") { collected[0] shouldEqual listOf(tStarred, tStarredUsed.withoutTag(USED)) }
                }
            }
        }
    }
})


/**
 *  Used to generate ids automatically, provide mocking of vernacularRepo.get(id) and retrieve entity data.
 *  * Provides minimal functionality to mock out VernacularRepo for the purpose of testing PlantNameSearchService.
 * */
class VernacularBag {
    private val vernaculars = mutableListOf<Vernacular>()
    private var nextId = 0L

    init {
        vernaculars.addAll(listOf(
            Vernacular(vernacular = "Untagged").apply { id = nextId++ },
            Vernacular(vernacular = "Starred").apply { id = nextId++ },
            Vernacular(vernacular = "Used").apply { id = nextId++ },
            Vernacular(vernacular = "StarredUsed").apply { id = nextId++ }
        ))
    }

    fun get(id: Long) = vernaculars.first { it.id == id }

    fun getIdsOf(vararg names: String): List<Long> {
        return mutableListOf<Long>().apply {
            names.forEach { name ->
                add(getByName(name).id)
            }
        }
    }

    fun getSearchResult(name: String, vararg tags: PlantNameTag = emptyArray()): SearchResult {
        return with(getByName(name)) {
            val tagFlags = tags.fold(COMMON.flag) { acc, cur -> acc or cur.flag }
            SearchResult(id, tagFlags, 0, name.capitalize())
        }
    }

    private fun getByName(name: String) = vernaculars.first { it.vernacular == name.capitalize() }
}


/**
 * Used to generate ids automatically, provide mocking of taxonRepo.get(id) and retrieve entity data.
 * Provides minimal functionality to mock out TaxonRepo for the purpose of testing PlantNameSearchService.
 * */
class TaxonBag {
    private val taxa = mutableListOf<Taxon>()
    private var nextId = 10L

    init {
        taxa.addAll(listOf(
            Taxon(generic = "Untagged").apply { id = nextId++ },
            Taxon(generic = "Starred").apply { id = nextId++ },
            Taxon(generic = "Used").apply { id = nextId++ },
            Taxon(generic= "StarredUsed").apply { id = nextId++ }
        ))
    }

    fun get(id: Long) = taxa.first { it.id == id }

    fun getIdsOf(vararg names: String): List<Long> {
        val ids = mutableListOf<Long>()
        names.forEach { name ->
            ids.add(getByName(name).id)
        }
        return ids
    }

    fun getSearchResult(name: String, vararg tags: PlantNameTag = emptyArray()): SearchResult {
        return with(getByName(name)) {
            val tagFlags = tags.fold(SCIENTIFIC.flag) { acc, cur -> acc or cur.flag }
            SearchResult(id, tagFlags, 0, name.capitalize())
        }
    }

    private fun getByName(name: String) = taxa.first { it.generic == name.capitalize() }
}

fun SearchResult.withoutTag(tag: PlantNameTag): SearchResult = copy(tags = tags and tag.flag.inv())
