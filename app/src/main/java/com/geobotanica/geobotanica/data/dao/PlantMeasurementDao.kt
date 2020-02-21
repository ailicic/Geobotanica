package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.PlantMeasurement

@Dao
interface PlantMeasurementDao : BaseDao<PlantMeasurement> {
    @Query("SELECT * FROM plantMeasurements WHERE id = :id")
    suspend fun get(id: Long): PlantMeasurement

    @Query("SELECT * FROM plantMeasurements WHERE plantId = :plantId AND type & :typeFlags != 0 " +
            "ORDER BY timestamp DESC")
    suspend fun getAllOfPlant(plantId: Long, typeFlags: Int): List<PlantMeasurement>

    @Query("SELECT * FROM plantMeasurements WHERE plantId = :plantId AND type & :typeFlags != 0 " +
            "ORDER BY timestamp DESC")
    fun getAllOfPlantLiveData(plantId: Long, typeFlags: Int): LiveData< List<PlantMeasurement> >

    @Query("SELECT * FROM plantMeasurements WHERE plantId = :plantId AND type & :typeFlags != 0 " +
            "ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastOfPlant(plantId: Long, typeFlags: Int): PlantMeasurement?

    @Query("SELECT * FROM plantMeasurements WHERE plantId = :plantId AND type & :typeFlags != 0  " +
            "ORDER BY timestamp DESC LIMIT 1")
    fun getLastOfPlantLiveData(plantId: Long, typeFlags: Int): LiveData<PlantMeasurement?>
}