package com.geobotanica.geobotanica.ui.map.marker

import com.geobotanica.geobotanica.util.Lg
import javax.inject.Inject

class PlanterMarkerDiffer @Inject constructor() {

    fun getDiffs(current: List<PlantMarkerData>, new: List<PlantMarkerData>): PlantMarkerDiffs {
        if (current.isEmpty()) { // Trivial case. Add all ids
            Lg.d("PlantMarkerDiffer: insert into empty: ${new.count()} ids")
            return PlantMarkerDiffs(toInsert = new)
        } else { // Compute diffs and apply
            val currentIds = current.map { it.plantId }
            val newIds = new.map { it.plantId }

            val idsToRemove = currentIds subtract newIds
            val idsToInsert = newIds subtract currentIds


            val idsToUpdate = mutableListOf<Long>()
            val idsForDeepComparison = currentIds intersect newIds
            idsForDeepComparison.forEach { id ->
                val currentId = current.first { it.plantId == id }
                val newId = new.first { it.plantId == id }
                if (currentId != newId)
                    idsToUpdate.add(id)
            }

            val idsNotChanged = idsForDeepComparison subtract idsToUpdate

            Lg.d("PlantMarkerDiffer: " +
                    "remove ${idsToRemove.count()}, " +
                    "insert ${idsToInsert.count()}, " +
                    "update ${idsToUpdate.count()}, " +
                    "keep ${idsNotChanged.count()}")

            val toRemove = current.filter { idsToRemove.contains(it.plantId) || idsToUpdate.contains(it.plantId) }
            val toInsert = new.filter { idsToInsert.contains(it.plantId) || idsToUpdate.contains(it.plantId) }

            return PlantMarkerDiffs(toRemove, toInsert)
        }
    }
}


data class PlantMarkerDiffs (
        val toRemove: List<PlantMarkerData> = emptyList(),
        val toInsert: List<PlantMarkerData> = emptyList()
)