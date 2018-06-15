package com.geobotanica.geobotanica.data.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.NonNull

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
    @NonNull val plantId: Long = 0L,
    @NonNull val latitude: Double? = null,
    @NonNull val longitude: Double? = null,
    @NonNull val altitude: Double? = null,
    @NonNull val precision: Float? = null,
    @NonNull val satellitesInUse: Int? = null,
    @NonNull val satellitesVisible: Int
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0
    @NonNull var timestamp: Long = 0L
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
