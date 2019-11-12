package com.geobotanica.geobotanica.data_taxa.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "vernaculars",
    indices = [
        Index(value = ["taxonId"]), // Required for Taxon foreign key constraint (c.f. PlantNameComposite)
        Index(value = ["vernacular"])
    ])
data class Vernacular(
    val taxonId: Long = 0L,
    val vernacular: String? = null
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
}