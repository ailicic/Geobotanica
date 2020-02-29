package com.geobotanica.geobotanica.data.entity

import androidx.room.*
import com.geobotanica.geobotanica.util.GbTime
import com.geobotanica.geobotanica.util.log2
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
    val timestamp: Instant = GbTime.now(),

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L
) {
    enum class Type(val flag: Int) {
        HEIGHT(         0b0000_0001), // 1
        DIAMETER(       0b0000_0010), // 2
        TRUNK_DIAMETER( 0b0000_0100), // 4
        ALL(            0b1111_1111);

        override fun toString() = when (this) {
            HEIGHT -> "Height"
            DIAMETER -> "Diameter"
            TRUNK_DIAMETER -> "Trunk diameter"
            ALL -> "All"
        }
    }
}

object MeasurementTypeConverter {
    @TypeConverter @JvmStatic
    fun toMeasurementType(flag: Int): PlantMeasurement.Type = PlantMeasurement.Type.values()[flag.log2()]

    @TypeConverter @JvmStatic
    fun fromMeasurementType(type: PlantMeasurement.Type): Int = type.flag
}