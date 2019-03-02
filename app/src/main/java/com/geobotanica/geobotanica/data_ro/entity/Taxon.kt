package com.geobotanica.geobotanica.data_ro.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "taxa"/*,
        indices = [ // Room crashes if uncommented since index already exists in imported db file
            Index(value = ["generic"]),
            Index(value = ["epithet"])
        ]*/)
data class Taxon(
        @PrimaryKey val id: Long = 0L,
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
    val latinName: String
        get() = "$generic $epithet"
}