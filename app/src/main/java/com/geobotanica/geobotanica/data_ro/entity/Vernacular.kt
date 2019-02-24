package com.geobotanica.geobotanica.data_ro.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vernaculars"/*,
        indices = [ // Room crashes if uncommented since index already exists in imported db file
            Index(value = ["taxonId"]),
            Index(value = ["vernacular"])
        ]*/)
data class  Vernacular(
    @PrimaryKey val id: Long = 0L,
    val taxonId: Long = 0L,
    val vernacular: String? = null
)