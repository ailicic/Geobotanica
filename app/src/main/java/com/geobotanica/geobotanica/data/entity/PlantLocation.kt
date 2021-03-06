package com.geobotanica.geobotanica.data.entity

import androidx.room.*
import com.geobotanica.geobotanica.android.location.Location

@Entity(tableName = "plant_locations",
    foreignKeys = [
        (ForeignKey(
                entity = Plant::class,
                parentColumns = ["id"],
                childColumns = ["plantId"],
                onDelete = ForeignKey.CASCADE
        ))
    ],
    indices = [
        (Index(value = ["plantId"]))
    ]
)

data class PlantLocation(
    val plantId: Long,
    @Embedded val location: Location,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L
)

val List<PlantLocation>.mostRecent: Location
    get() = this.maxBy { it.location.timestamp }?.location ?: throw IllegalStateException()
