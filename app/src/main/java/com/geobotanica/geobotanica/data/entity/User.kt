package com.geobotanica.geobotanica.data.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.NonNull

@Entity (tableName = "users")
data class User(
    @NonNull val nickname: String,
    @NonNull val timestamp: Long = 0
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0
}
