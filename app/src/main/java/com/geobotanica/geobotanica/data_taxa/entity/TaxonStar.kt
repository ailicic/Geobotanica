package com.geobotanica.geobotanica.data_taxa.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "taxonStars")
data class TaxonStar(
    @PrimaryKey val id: Long = 0L
)