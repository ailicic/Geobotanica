package com.geobotanica.geobotanica.data.entity

import android.arch.persistence.room.*
import android.support.annotation.NonNull
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
    @NonNull val userId: Long,
    @NonNull val plantId: Long,
    @NonNull val type: Int,
    @NonNull val fileName: String,
    @NonNull val timestamp: OffsetDateTime = OffsetDateTime.now()
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0

    enum class Type() {
        COMPLETE,
        LEAF,
        FLOWER,
        FRUIT,
        STEM,
        TRUNK;

        // TODO: Don't repeat these strings in strings.xml
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