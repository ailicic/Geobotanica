package com.geobotanica.geobotanica.data.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import com.geobotanica.geobotanica.data.entity.Location

@Dao
interface LocationDao : BaseDao<Location> {
    @Query("SELECT * FROM locations WHERE id=:id")
    fun get(id: Int): Location

    @Query("SELECT * FROM locations WHERE plantId = :plantId ORDER BY timestamp ASC LIMIT 1")
    fun plantLocation(plantId: Int): Location
}