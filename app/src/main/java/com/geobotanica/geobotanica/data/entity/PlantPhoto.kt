package com.geobotanica.geobotanica.data.entity

import androidx.room.*
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "plantPhotos",
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
data class PlantPhoto(
    val userId: Long,
    val plantId: Long,
    val type: Type,
    val fileName: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0

    enum class Type {
        COMPLETE,
        LEAF,
//        BUDS,
        FLOWER,
        FRUIT,
//        STEM,
        TRUNK;

        override fun toString() = when (this) {
            COMPLETE -> "Complete"
            LEAF -> "Leaf"
            FLOWER -> "Flower"
            FRUIT -> "Fruit"
            TRUNK -> "Trunk"
        }
    }

    enum class Action {
        RETAKE,
        DELETE;

        override fun toString() = when (this) {
            RETAKE -> "Retake"
            DELETE -> "Delete"
        }
    }
}

object PhotoTypeConverter {
    @TypeConverter @JvmStatic
    fun toPhotoType(ordinal: Int): PlantPhoto.Type = PlantPhoto.Type.values()[ordinal]

    @TypeConverter @JvmStatic
    fun fromPhotoType(type: PlantPhoto.Type): Int = type.ordinal
}