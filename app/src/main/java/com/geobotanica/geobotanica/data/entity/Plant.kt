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
    val scientificName: String? = null,
    val vernacularId: Long? = null,
    val taxonId: Long? = null,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0L

    enum class Type(val flag: Int) {
        TREE(   0b0000_0001),
        SHRUB(  0b0000_0010),
        HERB(   0b0000_0100),
        GRASS(  0b0000_1000),
        VINE(   0b0001_0000),
        FUNGUS( 0b0010_0000);

        override fun toString() = when (this) {
            TREE -> "Tree"
            SHRUB -> "Shrub"
            HERB -> "Herb"
            GRASS -> "Grass"
            VINE -> "Vine"
            FUNGUS -> "Fungus"
        }

        companion object {
            val allPlantTypeFlags = values().map { it.flag }.reduce { acc, it -> acc or it }

            fun fromFlag(flag: Int): Plant.Type = values().toList().first { it.flag and flag != 0 }

            fun flagsToList(plantTypeFlags: Int): List<Plant.Type> {
                val plantTypeList = mutableListOf<Plant.Type>()
                values().forEach {
                    if (plantTypeFlags and it.flag == it.flag)
                        plantTypeList.add(it)
                }
                return plantTypeList
            }
        }
    }
}

object PlantTypeConverter {
    @TypeConverter @JvmStatic
    fun toPlantType(ordinal: Int): Plant.Type = Plant.Type.values()[ordinal]

    @TypeConverter @JvmStatic
    fun fromPlantType(type: Plant.Type): Int = type.ordinal
}