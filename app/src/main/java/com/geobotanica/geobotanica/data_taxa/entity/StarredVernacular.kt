package com.geobotanica.geobotanica.data_taxa.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "starredVernaculars")
data class StarredVernacular(
    @PrimaryKey val id: Long = 0L
)