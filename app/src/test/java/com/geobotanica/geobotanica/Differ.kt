package com.geobotanica.geobotanica

import com.geobotanica.geobotanica.util.Differ
import com.geobotanica.geobotanica.util.Diffs
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe


object Differ : Spek({
    describe("Diffing id lists") {

        data class IdDiffTest(val currentIds: List<Long>, val newIds: List<Long>, val diffs: Diffs)
        listOf(
            IdDiffTest(emptyList(), listOf(1L), Diffs(insertIds = listOf(1L))),
            IdDiffTest(listOf(1L), emptyList(), Diffs(removeIds = listOf(1L))),
            IdDiffTest(listOf(1L), listOf(1L, 2L), Diffs(insertIds = listOf(2L))),
            IdDiffTest(listOf(1L, 2L), listOf(1L), Diffs(removeIds = listOf(2L))),
            IdDiffTest(listOf(1L), listOf(2L), Diffs(removeIds = listOf(1L), insertIds = listOf(2L)))
        ).forEach {
            context("Given ${it.currentIds} and new ${it.newIds}") {
                val diffs = Differ(currentIds = it.currentIds, newIds = it.newIds).getDiffs()

                it("Computes ${it.diffs}") {
                    diffs shouldEqual it.diffs
                }
            }
        }
    }
})
