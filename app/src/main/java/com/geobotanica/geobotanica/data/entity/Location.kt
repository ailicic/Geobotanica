package com.geobotanica.geobotanica.data.entity

import android.arch.persistence.room.*
import android.support.annotation.NonNull
import org.threeten.bp.OffsetDateTime
import java.io.Serializable

data class Location(
    @NonNull var latitude: Double? = null,
    @NonNull var longitude: Double? = null,
    @NonNull var altitude: Double? = null,
    @NonNull var precision: Float? = null,
    @NonNull var satellitesInUse: Int? = null,
    @NonNull var satellitesVisible: Int, // Before GPS fix, visible satellites are always available
    @NonNull var timestamp: OffsetDateTime = OffsetDateTime.now()
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
    @Embedded @NonNull var location: Location
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
}
