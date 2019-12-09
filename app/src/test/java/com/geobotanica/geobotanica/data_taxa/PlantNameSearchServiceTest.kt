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
import com.geobotanica.geobotanica.util.SpekExt.beforeEachBlockingTest
import com.geobotanica.geobotanica.util.SpekExt.setupTestDispatchers
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.collect
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SearchPlantNameServiceTest : Spek({
    val testDispatchers = setupTestDispatchers()

    val taxonRepo = mockk<TaxonRepo>()
    val vernacularRepo = mockk<VernacularRepo>()

    val plantNameSearchService by memoized { PlantNameSearchService(testDispatchers, taxonRepo, vernacularRepo) }

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
                beforeEachBlockingTest(testDispatchers) {
                    plantNameSearchService.search("").collect { collected.add(it) }
                }

                it("Should return nothing") {
                    collected.forEach { it shouldEqual emptyList() }
                }
            }
        }

        context("Populated db") {

            context("Default search") {
                val vStarredUsed = vBag.getSearchResult("V StarredUsed", STARRED, USED)
                val vStarred = vBag.getSearchResult("V Starred", STARRED)
                val vUsed = vBag.getSearchResult("V Used", USED)

                val tStarredUsed = tBag.getSearchResult("T StarredUsed", STARRED, USED)
                val tStarred = tBag.getSearchResult("T Starred", STARRED)
                val tUsed = tBag.getSearchResult("T Used", USED)

                beforeEachTest {
                    coEvery { vernacularRepo.getAllStarred(any()) } returns vBag.getIdsOf("V Starred", "V StarredUsed")
                    coEvery { vernacularRepo.getAllUsed(any()) } returns vBag.getIdsOf("V Used", "V StarredUsed")
                    coEvery { taxonRepo.getAllStarred(any()) } returns tBag.getIdsOf("T Starred", "T StarredUsed")
                    coEvery { taxonRepo.getAllUsed(any()) } returns tBag.getIdsOf("T Used", "T StarredUsed")
                }

                context("No search filter") {
                    beforeEachBlockingTest(testDispatchers) {
                        plantNameSearchService.search("").collect { collected.add(it) }
                    }
                    it("1st emission correct") {
                        collected[0] shouldContainAll listOf(vStarredUsed, vStarred)
                    }
                    it("2nd emission correct") {
                        collected[1] shouldContainAll listOf(vStarredUsed, vStarred, vUsed)
                    }
                    it("3rd emission correct") {
                        collected[2] shouldContainAll listOf(tStarredUsed, vStarredUsed, tStarred, vStarred, vUsed)
                    }
                    it("4th emission correct") {
                        collected[3] shouldContainAll listOf(tStarredUsed, vStarredUsed, tStarred, vStarred, tUsed, vUsed)
                    }
                }

                context("Starred filtered out") {
                    beforeEachBlockingTest(testDispatchers) {
                        plantNameSearchService.search("", SearchFilterOptions(STARRED.flag))
                                .collect { collected.add(it) }
                    }

                    it("Should skip starred searches") { collected.size shouldEqual 2 }
                    it("1st emission correct") {
                        collected[0] shouldContainAll listOf(vUsed, vStarredUsed.withoutTag(STARRED))
                    }
                    it("2nd emission correct") {
                        collected[1] shouldContainAll listOf(vUsed, vStarredUsed.withoutTag(STARRED), tUsed, tStarredUsed.withoutTag(STARRED))
                    }
                }

                context("Used filtered out") {
                    beforeEachBlockingTest(testDispatchers) {
                        plantNameSearchService.search("", SearchFilterOptions(USED.flag))
                                .collect { collected.add(it) }
                    }

                    it("Should skip used searches") { collected.size shouldEqual 2 }
                    it("1st emission correct") {
                        collected[0] shouldContainAll listOf(vStarred, vStarredUsed.withoutTag(USED))
                    }
                    it("2nd emission correct") {
                        collected[1] shouldContainAll listOf(vStarred, vStarredUsed.withoutTag(USED), tStarred, tStarredUsed.withoutTag(USED))
                    }
                }

                context("Vernaculars filtered out") {
                    beforeEachBlockingTest(testDispatchers) {
                        plantNameSearchService.search("", SearchFilterOptions(COMMON.flag))
                                .collect { collected.add(it) }
                    }

                    it("Should skip vernacular searches") { collected.size shouldEqual 2 }
                    it("1st emission correct") { collected[0] shouldContainAll listOf(tStarred, tStarredUsed) }
                    it("2nd emission correct") { collected[1] shouldContainAll listOf(tStarredUsed, tStarred, tUsed) }
                }

                context("Taxa filtered out") {
                    beforeEachBlockingTest(testDispatchers) {
                        plantNameSearchService.search("", SearchFilterOptions(SCIENTIFIC.flag))
                                .collect { collected.add(it) }
                    }

                    it("Should skip taxa searches") { collected.size shouldEqual 2 }
                    it("1st emission correct") { collected[0] shouldContainAll listOf(vStarred, vStarredUsed) }
                    it("2nd emission correct") { collected[1] shouldContainAll listOf(vStarredUsed, vStarred, vUsed) }
                }

                context("Starred and taxa filtered out") {
                    beforeEachBlockingTest(testDispatchers) {
                        plantNameSearchService.search(
                                "", SearchFilterOptions(STARRED.flag or SCIENTIFIC.flag)
                        ).collect { collected.add(it) }
                    }

                    it("Should skip starred/taxa searches") { collected.size shouldEqual 1 }
                    it("1st emission correct") { collected[0] shouldContainAll listOf(vUsed, vStarredUsed.withoutTag(STARRED)) }
                }

                context("Used and vernaculars filtered out") {
                    beforeEachBlockingTest(testDispatchers) {
                        plantNameSearchService.search(
                                "", SearchFilterOptions(USED.flag or COMMON.flag)
                        ).collect { collected.add(it) }
                    }

                    it("Should skip used/vernacular searches") { collected.size shouldEqual 1 }
                    it("1st emission correct") { collected[0] shouldContainAll listOf(tStarred, tStarredUsed.withoutTag(USED)) }
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
            Vernacular(vernacular = "V StarredUsed").apply { id = nextId++ },
            Vernacular(vernacular = "V Starred").apply { id = nextId++ },
            Vernacular(vernacular = "V Used").apply { id = nextId++ },
            Vernacular(vernacular = "V Untagged").apply { id = nextId++ }
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
            Taxon(generic= "T StarredUsed").apply { id = nextId++ },
            Taxon(generic = "T Starred").apply { id = nextId++ },
            Taxon(generic = "T Used").apply { id = nextId++ },
            Taxon(generic = "T Untagged").apply { id = nextId++ }
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
