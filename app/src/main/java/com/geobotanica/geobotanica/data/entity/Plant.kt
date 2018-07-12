package com.geobotanica.geobotanica.data.entity

import android.arch.persistence.room.*
import android.support.annotation.NonNull
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
data class Plant(
    val userId: Long = 0L,
    val type: Type,
    val commonName: String? = null,
    val latinName: String? = null,
    @NonNull val timestamp: OffsetDateTime = OffsetDateTime.now()
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
                VINE -> "Fruit"
        }
    }
}


object PhotoTypeConverter {
    @TypeConverter @JvmStatic
    fun toPhotoType(ordinal: Int): Photo.Type = Photo.Type.values()[ordinal]

    @TypeConverter @JvmStatic
    fun fromPhotoType(type: Photo.Type): Int = type.ordinal
}