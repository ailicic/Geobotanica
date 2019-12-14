package com.geobotanica.geobotanica.data.entity

import androidx.room.*
import com.geobotanica.geobotanica.util.GbTime
import org.threeten.bp.Instant

@Entity(tableName = "plantMeasurements",
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
        Index(value = ["userId"]),
        Index(value = ["plantId"])
    ]
)
data class PlantMeasurement(
    val userId: Long,
    val plantId: Long,
    val type: Type,
    val measurement: Float, // cm
    val timestamp: Instant = GbTime.now()
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    enum class Type {
        HEIGHT,
        DIAMETER,
        TRUNK_DIAMETER;

        override fun toString() = when (this) {
            HEIGHT -> "Height"
            DIAMETER -> "Diameter"
            TRUNK_DIAMETER -> "Trunk diameter"
        }
    }
}

object MeasurementTypeConverter {
    @TypeConverter @JvmStatic
    fun toMeasurementType(ordinal: Int): PlantMeasurement.Type = PlantMeasurement.Type.values()[ordinal]

    @TypeConverter @JvmStatic
    fun fromMeasurementType(type: PlantMeasurement.Type): Int = type.ordinal
}