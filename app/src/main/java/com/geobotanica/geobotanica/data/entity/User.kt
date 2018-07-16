package com.geobotanica.geobotanica.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity (tableName = "users")
data class User(
    val nickname: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0
}
