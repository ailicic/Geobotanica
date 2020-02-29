package com.geobotanica.geobotanica.data.entity

import androidx.room.*
import com.geobotanica.geobotanica.data.entity.Plant.Type.*
import com.geobotanica.geobotanica.util.GbTime
import org.threeten.bp.Instant
import com.geobotanica.geobotanica.util.log2

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
    val filename: String,
    val timestamp: Instant = GbTime.now(),

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L
) {
    enum class Type(val flag: Int) {
        COMPLETE(   0b0000_0001), // 1
        LEAF(       0b0000_0010), // 2
        FLOWER(     0b0000_0100), // 4
        FRUIT(      0b0000_1000), // 8
        TRUNK(      0b0001_0000); // 16
//        BUDS,
//        STEM,

        override fun toString() = when (this) {
            COMPLETE -> "Complete"
            LEAF -> "Leaf"
            FLOWER -> "Flower"
            FRUIT -> "Fruit"
            TRUNK -> "Trunk"
        }
    }

    companion object {
        fun typesValidFor(plantType: Plant.Type) =
            when (plantType){
                TREE ->     listOf(Type.COMPLETE, Type.LEAF, Type.FLOWER, Type.FRUIT, Type.TRUNK)
                SHRUB ->    listOf(Type.COMPLETE, Type.LEAF, Type.FLOWER, Type.FRUIT)
                HERB ->     listOf(Type.COMPLETE, Type.LEAF, Type.FLOWER, Type.FRUIT)
                GRASS ->    listOf(Type.COMPLETE)
                VINE ->     listOf(Type.COMPLETE, Type.LEAF, Type.FLOWER, Type.FRUIT)
                FUNGUS ->   listOf(Type.COMPLETE)
            }

    }

}

fun List<PlantPhoto>.selectMain(): PlantPhoto {
    return sortedByDescending { it.timestamp }.run {
        firstOrNull { it.type == PlantPhoto.Type.COMPLETE } ?: first()
    }
}

object PhotoTypeConverter {
    @TypeConverter @JvmStatic
    fun toPhotoType(flag: Int): PlantPhoto.Type = PlantPhoto.Type.values()[flag.log2()]

    @TypeConverter @JvmStatic
    fun fromPhotoType(type: PlantPhoto.Type): Int = type.flag
}