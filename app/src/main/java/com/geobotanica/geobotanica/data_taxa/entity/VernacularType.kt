package com.geobotanica.geobotanica.data_taxa.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "vernacularTypes",
        indices = [
            Index(value = ["vernacular"])
        ])
data class VernacularType(
    val plantTypeFlags: Int,
    val vernacular: String
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
}