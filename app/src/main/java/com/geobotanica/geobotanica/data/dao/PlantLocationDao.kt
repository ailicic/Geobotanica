package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.PlantLocation

@Dao
interface PlantLocationDao : BaseDao<PlantLocation> {
    @Query("SELECT * FROM plant_locations WHERE id = :id")
    suspend fun get(id: Long): PlantLocation

    @Query("SELECT * FROM plant_locations WHERE id = :id")
    fun getLiveData(id: Long): LiveData<PlantLocation>

    @Query("SELECT * FROM plant_locations WHERE plantId = :plantId ORDER BY timestamp ASC") // TODO: Check ordering
    fun getPlantLocations(plantId: Long): LiveData<List<PlantLocation>>

    @Query("SELECT * FROM plant_locations WHERE plantId = :plantId ORDER BY timestamp ASC LIMIT 1")
    fun getLastPlantLocation(plantId: Long): LiveData<PlantLocation>
}