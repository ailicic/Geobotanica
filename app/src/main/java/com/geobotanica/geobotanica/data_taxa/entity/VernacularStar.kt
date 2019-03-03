package com.geobotanica.geobotanica.data_taxa.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vernacularStars")
data class VernacularStar(
    @PrimaryKey val id: Long = 0L
)