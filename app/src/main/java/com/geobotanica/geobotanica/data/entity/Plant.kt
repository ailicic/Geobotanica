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
        TREE(   0b0000_0001), // 1
        SHRUB(  0b0000_0010), // 2
        HERB(   0b0000_0100), // 4
        GRASS(  0b0000_1000), // 8
        VINE(   0b0001_0000), // 16
        FUNGUS( 0b0010_0000); // 32

        override fun toString() = when (this) {
            TREE -> "Tree"
            SHRUB -> "Shrub"
            HERB -> "Herb"
            GRASS -> "Grass"
            VINE -> "Vine"
            FUNGUS -> "Fungus"
        }

        companion object {
            val allTypeFlags = values().map { it.flag }.reduce { acc, it -> acc or it }
            val onlyPlantTypeFlags = allTypeFlags xor FUNGUS.flag

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