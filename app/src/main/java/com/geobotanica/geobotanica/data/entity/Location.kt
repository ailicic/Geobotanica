package com.geobotanica.geobotanica.data.entity

import android.arch.persistence.room.*
import org.threeten.bp.OffsetDateTime
import java.io.Serializable

data class Location(
    var latitude: Double? = null,
    var longitude: Double? = null,
    var altitude: Double? = null,
    var precision: Float? = null,
    var satellitesInUse: Int? = null,
    var satellitesVisible: Int, // Before GPS fix, visible satellites are always available
    var timestamp: OffsetDateTime = OffsetDateTime.now()
): Serializable


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
    var plantId: Long,
    @Embedded var location: Location
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
}
