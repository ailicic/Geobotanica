package com.geobotanica.geobotanica.data.entity

import android.arch.persistence.room.*
import android.support.annotation.NonNull

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
        @NonNull val userId: Long = 0L,
        @NonNull val type: Int,
        val commonName: String? = null,
        val latinName: String? = null,
        @NonNull val timestamp: Long = 0L
) {
        @PrimaryKey(autoGenerate = true) var id: Long = 0

        enum class Type() {
                TREE,
                SHRUB,
                HERB,
                VINE;

                // TODO: Don't repeat these strings in strings.xml
                override fun toString() = when (this) {
                        TREE -> "Tree"
                        SHRUB -> "Shrub"
                        HERB -> "Herb"
                        VINE -> "Fruit"
                }
        }
}