package com.geobotanica.geobotanica.data.entity

import androidx.room.*
import com.geobotanica.geobotanica.util.GbTime
import com.geobotanica.geobotanica.util.log2
import org.threeten.bp.Instant

@Entity(tableName = "plants",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE)
    ],
    indices = [ Index(value = ["userId"]) ]
)
data class Plant(
    val userId: Long = 0L,
    val type: Type,
    val commonName: String? = null,
    val scientificName: String? = null,
    val vernacularId: Long? = null,
    val taxonId: Long? = null,
    val timestamp: Instant = GbTime.now(),

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L
) {
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

            fun fromFlag(flag: Int): Type = values().toList().first { it.flag and flag != 0 }

            fun flagsToList(plantTypeFlags: Int): List<Type> {
                val plantTypeList = mutableListOf<Type>()
                values().forEach {
                    if (plantTypeFlags and it.flag != 0)
                        plantTypeList.add(it)
                }
                return plantTypeList
            }
        }
    }
}

object PlantTypeConverter {
    @TypeConverter @JvmStatic
    fun toPlantType(flag: Int): Plant.Type = Plant.Type.values()[flag.log2()]

    @TypeConverter @JvmStatic
    fun fromPlantType(type: Plant.Type): Int = type.flag
}