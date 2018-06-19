package com.geobotanica.geobotanica.data.entity

import android.arch.persistence.room.*
import android.support.annotation.NonNull

@Entity(tableName = "measurements",
        foreignKeys = [
                ForeignKey(
                        entity = User::class,
                        parentColumns = ["id"],
                        childColumns = ["userId"],
                        onDelete = ForeignKey.CASCADE),
                ForeignKey(
                        entity = Plant::class,
                        parentColumns = ["id"],
                        childColumns = ["plantId"],
                        onDelete = ForeignKey.CASCADE)
        ],
        indices = [
                Index(value = ["plantId"])
        ]
)
data class Measurement(
    @NonNull val userId: Long,
    @NonNull val plantId: Long,
    @NonNull val type: Int,
    @NonNull val measurement: Float, // cm
    @NonNull val timestamp: Long = 0L
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    enum class Type() {
        HEIGHT,
        DIAMETER,
        TRUNK_DIAMETER,
        FLOWER,
        FRUIT;

        override fun toString() = when (this) {
            HEIGHT -> "Height"
            DIAMETER -> "Diameter"
            TRUNK_DIAMETER -> "Trunk diameter"
            FLOWER -> "Flower"
            FRUIT -> "Fruit"
        }
    }
}