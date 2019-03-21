package com.geobotanica.geobotanica.data_taxa.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// NOTE: Inserts by room create ids by incrementing the largest id present, regardless of gaps in the ids. Could use this to detect user-entered data.

@Entity(tableName = "taxa"/*,
        indices = [ // Room crashes if uncommented since index already exists in imported db file
            Index(value = ["generic"]),
            Index(value = ["epithet"])
        ]*/)
data class Taxon(
    val kingdom: String? = null,
    val phylum: String? = null,
    val class_: String? = null,
    val order: String? = null,
    val family: String? = null,
    val generic: String? = null, // Latin 1 (genericName)
    val genus: String? = null,
    val epithet: String? = null, // Latin 2 (specificEpithet)
    val infraspecificEpithet: String? = null
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L

    val scientific: String
        get() = "$generic $epithet"

    enum class Kingdom {
        PLANTS, FUNGI;

        override fun toString(): String =
            when (this) {
                PLANTS -> "plantae"
                FUNGI -> "fungi"
            }
    }
}