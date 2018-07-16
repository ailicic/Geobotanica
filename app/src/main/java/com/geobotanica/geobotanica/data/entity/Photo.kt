package com.geobotanica.geobotanica.data.entity

import androidx.room.*
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "photos",
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
data class Photo(
    val userId: Long,
    val plantId: Long,
    val type: Type,
    val fileName: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0

    enum class Type() {
        COMPLETE,
        LEAF,
        FLOWER,
        FRUIT,
        STEM,
        TRUNK;

        override fun toString() = when (this) {
            COMPLETE -> "Complete"
            LEAF -> "Leaf"
            FLOWER -> "Flower"
            FRUIT -> "Fruit"
            STEM -> "Stem"
            TRUNK -> "Trunk"
        }
    }
}

object PlantTypeConverter {
    @TypeConverter @JvmStatic
    fun toPlantType(ordinal: Int): Plant.Type = Plant.Type.values()[ordinal]

    @TypeConverter @JvmStatic
    fun fromPlantType(type: Plant.Type): Int = type.ordinal
}