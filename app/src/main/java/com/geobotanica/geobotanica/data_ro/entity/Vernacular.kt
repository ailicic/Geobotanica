package com.geobotanica.geobotanica.data_ro.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "vernaculars",
    foreignKeys = [ForeignKey(
        entity = Taxon::class,
        parentColumns = ["id"],
        childColumns = ["taxonId"],
        onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["taxonId"]) // Required for Taxon foreign key constraint (c.f. PlantNameComposite)
    ])
data class  Vernacular(
    @PrimaryKey val id: Long = 0L,
    val taxonId: Long = 0L,
    val vernacular: String? = null
)