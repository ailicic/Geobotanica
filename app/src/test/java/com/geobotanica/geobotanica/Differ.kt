package com.geobotanica.geobotanica

import com.geobotanica.geobotanica.util.Differ
import com.geobotanica.geobotanica.util.Diffs
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object Differ : Spek({
    describe("Diffing id lists") {

        context("Empty list provided with new ids") {
            val diffs = Differ(currentIds = emptyList(), newIds = listOf(1L, 2L)).getDiffs()

            it("Should insert all ids") {
                diffs shouldEqual Diffs(insertIds = listOf(1L, 2L))
            }
        }
    }
})
