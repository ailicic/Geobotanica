package com.geobotanica.geobotanica.util

data class Diffs (
        val removeIds: List<Long> = emptyList(),
        val insertIds: List<Long> = emptyList()
)

object IdDiffer {
    fun computeDiffs(currentIds: List<Long>, newIds: List<Long>): Diffs {
        if (currentIds.isEmpty()) { // Trivial case. Add all ids
            Lg.d("IdDiffer: insert into empty: ${newIds.count()} ids")
            return Diffs(insertIds = newIds)
        } else { // Compute diffs and apply
            val idsToRemove = currentIds subtract newIds
            val idsToInsert = newIds subtract currentIds
//            val idsToUpdate = emptyList<Long>()  // TODO: Need a deep comparison to detect updated markers
            val idsNotChanged = currentIds intersect newIds
            Lg.d("IdDiffer: remove ${idsToRemove.count()}")
            Lg.d("IdDiffer: insert ${idsToInsert.count()}")
//            Lg.d("Diff:: update ${idsToUpdate.count()}")
            Lg.d("IdDiffer: keep ${idsNotChanged.count()}")
//            idsToRemove += idsToUpdate // Updated markers get removed, then inserted
//            idsToInsert += idsToUpdate
            return Diffs(idsToRemove.toList(), idsToInsert.toList())
        }
    }
}