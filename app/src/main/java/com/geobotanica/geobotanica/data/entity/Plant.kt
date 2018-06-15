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
        @NonNull val userId: Long = 0,
        @NonNull val type: String,
        val commonName: String? = null,
        val latinName: String? = null,
        @NonNull val timestamp: Long = 0
) {
        @PrimaryKey(autoGenerate = true) var id: Long = 0
}