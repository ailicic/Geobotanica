package com.geobotanica.geobotanica.data_taxa.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "taxonTypes",
        indices = [
            Index(value = ["order"]),
            Index(value = ["family"]),
            Index(value = ["genus"]),
            Index(value = ["epithet"])
        ])
data class TaxonType(
    val plantTypeFlags: Int,
    val order: String? = null,
    val family: String? = null,
    val genus: String? = null,
    val epithet: String? = null
) {
    @PrimaryKey(autoGenerate = true) var vascanId: Long = 0L
}