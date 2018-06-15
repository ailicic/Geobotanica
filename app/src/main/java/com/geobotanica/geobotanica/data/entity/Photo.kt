package com.geobotanica.geobotanica.data.entity

import android.arch.persistence.room.*
import android.support.annotation.NonNull

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
    @NonNull val fileName: String,
    @NonNull val photoType: String,
    @NonNull val timestamp: Long = 0
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0
}