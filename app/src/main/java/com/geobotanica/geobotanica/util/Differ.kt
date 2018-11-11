package com.geobotanica.geobotanica.util


data class Diffs (
        val removeIds: List<Long> = emptyList(),
        val insertIds: List<Long> = emptyList()
)

class Differ(val currentIds: List<Long>, val newIds: List<Long>) {
    fun getDiffs(): Diffs {
        if (currentIds.isEmpty()) { // Trivial case. Add all ids
            Lg.d("Differ: insert ${newIds.count()} ids")
            return Diffs(insertIds = newIds)
        } else { // Compute diffs and apply
            var idsToRemove = currentIds subtract newIds
            var idsToInsert = newIds subtract currentIds
//            val idsToUpdate = emptyList<Long>()  // TODO: Need a deep comparison to detect updated markers
            val idsNotChanged = currentIds intersect newIds
            Lg.d("Diff:: remove ${idsToRemove.count()}")
            Lg.d("Diff:: insert ${idsToInsert.count()}")
//            Lg.d("Diff:: update ${idsToUpdate.count()}")
            Lg.d("Diff:: keep ${idsNotChanged.count()}")
//            idsToRemove += idsToUpdate // Updated markers get removed, then inserted
//            idsToInsert += idsToUpdate
            return Diffs(idsToRemove.toList(), idsToInsert.toList())
        }
    }
}