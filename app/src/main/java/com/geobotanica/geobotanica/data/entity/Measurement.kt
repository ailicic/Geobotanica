package com.geobotanica.geobotanica.data.entity

import androidx.room.*
import org.threeten.bp.OffsetDateTime

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
        Index(value = ["plantId"]),
        Index(value = ["userId"])
    ]
)
data class Measurement(
    val userId: Long,
    val plantId: Long,
    val type: Type,
    val measurement: Float, // cm
    val timestamp: OffsetDateTime = OffsetDateTime.now()
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    enum class Type {
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

    enum class Unit { CM, M, IN, FT }
}

object MeasurementTypeConverter {
    @TypeConverter @JvmStatic
    fun toMeasurementType(ordinal: Int): Measurement.Type = Measurement.Type.values()[ordinal]

    @TypeConverter @JvmStatic
    fun fromMeasurementType(type: Measurement.Type): Int = type.ordinal
}