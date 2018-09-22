package com.geobotanica.geobotanica.data.entity

import androidx.room.*
import org.threeten.bp.OffsetDateTime
import java.io.Serializable

data class Location(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val precision: Float? = null,
    val satellitesInUse: Int? = null,
    val satellitesVisible: Int, // Before GPS fix, visible satellites are always available
    val timestamp: OffsetDateTime = OffsetDateTime.now()
): Serializable {
    fun isRecent(): Boolean = OffsetDateTime.now().minusSeconds(1).isBefore(this.timestamp)
}


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
    @Embedded val location: Location
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
}
