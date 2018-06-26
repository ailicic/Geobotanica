package com.geobotanica.geobotanica.data.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.NonNull
import org.threeten.bp.OffsetDateTime
import java.io.Serializable

@Entity(tableName = "locations",
        foreignKeys = [
                ForeignKey(
                    entity = Plant::class,
                    parentColumns = ["id"],
                    childColumns = ["plantId"],
                    onDelete = ForeignKey.CASCADE
                )
        ],
        indices = [
                Index( value = ["plantId"] )
        ]
)
data class Location(
    @NonNull var plantId: Long? = null,
    @NonNull var latitude: Double? = null,
    @NonNull var longitude: Double? = null,
    @NonNull var altitude: Double? = null,
    @NonNull var precision: Float? = null,
    @NonNull var satellitesInUse: Int? = null,
    @NonNull var satellitesVisible: Int,
    @NonNull var timestamp: OffsetDateTime = OffsetDateTime.now()
): Serializable {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L
}

//enum class LocationType {
//    CURRENT_GPS_LOCATION,
//    BEST_GPS_LOCATION,
//    NETWORK_LOCATION,
//    CACHED_LOCATION;
//
//    override fun toString() =  when(this) {
//        CURRENT_GPS_LOCATION -> "Current GPS"
//        BEST_GPS_LOCATION-> "Best GPS"
//        NETWORK_LOCATION -> "Network"
//        CACHED_LOCATION -> "Cached"
//    }
//}
