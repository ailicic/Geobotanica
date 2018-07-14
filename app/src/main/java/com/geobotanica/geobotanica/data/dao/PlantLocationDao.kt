package com.geobotanica.geobotanica.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import com.geobotanica.geobotanica.data.entity.PlantLocation

@Dao
interface PlantLocationDao : BaseDao<PlantLocation> {
    @Query("SELECT * FROM plant_locations WHERE id = :id")
    fun get(id: Long): LiveData<PlantLocation>

    @Query("SELECT * FROM plant_locations WHERE plantId = :plantId ORDER BY timestamp ASC LIMIT 1")
    fun getPlantLocation(plantId: Long): LiveData<PlantLocation>
}