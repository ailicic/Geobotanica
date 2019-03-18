package com.geobotanica.geobotanica.data_taxa.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "taxonTypes")
data class TaxonType(
    val plantTypeFlags: Int,
    val order: String? = null,
    val family: String? = null,
    val genus: String? = null,
    val epithet: String? = null
) {
    @PrimaryKey(autoGenerate = true) var vascanId: Long = 0L
}