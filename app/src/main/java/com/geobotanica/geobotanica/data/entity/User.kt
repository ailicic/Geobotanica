package com.geobotanica.geobotanica.data.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity (tableName = "users")
data class User(
    val nickname: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
) {
    @PrimaryKey(autoGenerate = true) var id: Long = 0

    @Ignore constructor(id: Long = 0, nickname: String): this(nickname) { // TODO: Remove this after Login screen created
        this.id = id
    }
}
