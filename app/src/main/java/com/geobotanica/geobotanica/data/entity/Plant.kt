package com.geobotanica.geobotanica.data.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
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
        @NonNull val userId: Long = 0L,
        @NonNull val type: Int,
        @NonNull val commonName: String = "",
        @NonNull val latinName: String = "",
        @NonNull val timestamp: OffsetDateTime = OffsetDateTime.now()
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