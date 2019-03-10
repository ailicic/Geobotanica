package com.geobotanica.geobotanica.data_taxa.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    val tag: Int,
    val vernacularId: Long? = null,
    val taxonId: Long? = null
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
}
