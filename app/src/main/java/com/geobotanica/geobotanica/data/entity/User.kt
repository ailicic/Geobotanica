package com.geobotanica.geobotanica.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.geobotanica.geobotanica.util.GbTime
import org.threeten.bp.Instant


@Entity (tableName = "users")
data class User(
    val nickname: String,
    val timestamp: Instant = GbTime.now()
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0
}
