package com.geobotanica.geobotanica.data_taxa.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "starredTaxa")
data class StarredTaxon(
    @PrimaryKey val id: Long = 0L
)