package com.geobotanica.geobotanica.data.entity

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.NonNull
import org.threeten.bp.OffsetDateTime

@Entity (tableName = "users")
data class User(
    @NonNull val nickname: String,
    @NonNull val timestamp: OffsetDateTime = OffsetDateTime.now()
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0
}
