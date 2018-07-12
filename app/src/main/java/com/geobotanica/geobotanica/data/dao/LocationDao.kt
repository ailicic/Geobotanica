package com.geobotanica.geobotanica.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import com.geobotanica.geobotanica.data.entity.Location

@Dao
interface LocationDao : BaseDao<Location> {
    @Query("SELECT * FROM locations WHERE id = :id")
    fun get(id: Long): LiveData<Location>

    @Query("SELECT * FROM locations WHERE plantId = :plantId ORDER BY timestamp ASC LIMIT 1")
    fun getPlantLocation(plantId: Long): LiveData<Location>
}