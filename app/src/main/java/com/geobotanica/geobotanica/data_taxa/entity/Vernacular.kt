package com.geobotanica.geobotanica.data_taxa.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vernaculars"/*,
    indices = [ // Room crashes if uncommented since index already exists in imported db file
        Index(value = ["taxonId"]) // Required for Taxon foreign key constraint (c.f. PlantNameComposite)
    ]*/)
data class Vernacular(
    val taxonId: Long = 0L,
    val vernacular: String? = null
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
}