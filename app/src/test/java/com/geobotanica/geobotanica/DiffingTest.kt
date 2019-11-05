package com.geobotanica.geobotanica

import com.geobotanica.geobotanica.ui.map.marker.PlantMarkerData
import com.geobotanica.geobotanica.ui.map.marker.PlantMarkerDiffs
import com.geobotanica.geobotanica.ui.map.marker.PlanterMarkerDiffer
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe


object PlantMarkerDifferTest : Spek({

    val plant1 = PlantMarkerData(1L)
    val plant1updated = PlantMarkerData(1L).copy(commonName = "updated")
    val plant2 = PlantMarkerData(2L)
    val plant3 = PlantMarkerData(3L)

    data class DiffTest(
            val description: String,
            val current: List<PlantMarkerData>,
            val new: List<PlantMarkerData>,
            val diffs: PlantMarkerDiffs
    )

    val planterMarkerDiffer = PlanterMarkerDiffer()

    listOf(
            DiffTest("Add one to empty", emptyList(), listOf(plant1), PlantMarkerDiffs(toInsert = listOf(plant1))),
            DiffTest("Add two to empty", emptyList(), listOf(plant1, plant2), PlantMarkerDiffs(toInsert = listOf(plant1, plant2))),
            DiffTest("Add one to one", listOf(plant1), listOf(plant1, plant2), PlantMarkerDiffs(toInsert = listOf(plant2))),
            DiffTest("Add one, remove one", listOf(plant1), listOf(plant2), PlantMarkerDiffs(toRemove = listOf(plant1), toInsert = listOf(plant2))),
            DiffTest("Remove one from two", listOf(plant1, plant2), listOf(plant1), PlantMarkerDiffs(toRemove = listOf(plant2))),
            DiffTest("Remove all two", listOf(plant1, plant2), emptyList(), PlantMarkerDiffs(toRemove = listOf(plant1, plant2))),
            DiffTest("Remove last one", listOf(plant1), emptyList(), PlantMarkerDiffs(toRemove = listOf(plant1))),
            DiffTest("Update one of one",
                    listOf(plant1),
                    listOf(plant1updated),
                    PlantMarkerDiffs(toRemove = listOf(plant1), toInsert = listOf(plant1updated))
            ),
            DiffTest("Update one of two",
                    listOf(plant1, plant2),
                    listOf(plant1updated, plant2),
                    PlantMarkerDiffs(toRemove = listOf(plant1), toInsert = listOf(plant1updated))
            ),
            DiffTest("Update one, remove one",
                    listOf(plant1, plant2),
                    listOf(plant1updated),
                    PlantMarkerDiffs(toRemove = listOf(plant1, plant2), toInsert = listOf(plant1updated))
            ),
            DiffTest("Update one, remove one, keep one",
                    listOf(plant1, plant2, plant3),
                    listOf(plant1updated, plant3),
                    PlantMarkerDiffs(toRemove = listOf(plant1, plant2), toInsert = listOf(plant1updated))
            )
    ).forEach {
        describe(it.description) {

            val diffs = planterMarkerDiffer.getDiffs(current = it.current, new = it.new)

            it("Should compute diff correctly") {
                diffs shouldEqual it.diffs
            }
        }
    }
})
