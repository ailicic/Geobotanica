package com.geobotanica.geobotanica.data.entity

import androidx.room.*
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "plants",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE)
    ],
    indices = [ Index(value = ["userId"]) ]
)
data class  Plant(
    val userId: Long = 0L,
    val type: Type,
    val commonName: String? = null,
    val latinName: String? = null,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0

    enum class Type {
        TREE,
        SHRUB,
        HERB,
        GRASS,
        VINE;

        override fun toString() = when (this) {
            TREE -> "Tree"
            SHRUB -> "Shrub"
            HERB -> "Herb"
            GRASS -> "Grass"
            VINE -> "Vine"
        }
    }
}

object PlantTypeConverter {
    @TypeConverter @JvmStatic
    fun toPlantType(ordinal: Int): Plant.Type = Plant.Type.values()[ordinal]

    @TypeConverter @JvmStatic
    fun fromPlantType(type: Plant.Type): Int = type.ordinal
}