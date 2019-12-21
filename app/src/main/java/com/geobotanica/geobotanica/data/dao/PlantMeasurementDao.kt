package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.PlantMeasurement

@Dao
interface PlantMeasurementDao : BaseDao<PlantMeasurement> {
    @Query("SELECT * FROM plantMeasurements WHERE id = :id")
    fun get(id: Long): LiveData<PlantMeasurement>

    @Query("SELECT * FROM plantMeasurements WHERE plantId = :plantId ORDER BY timestamp DESC")
    fun getAllMeasurementsOfPlantLiveData(plantId: Long): LiveData< List<PlantMeasurement> >

    @Query("SELECT * FROM plantMeasurements WHERE plantId = :plantId AND type = :type " +
            "ORDER BY timestamp DESC")
    fun getMeasurementsOfPlant(plantId: Long, type: Int): List<PlantMeasurement>

    @Query("SELECT * FROM plantMeasurements WHERE plantId = :plantId AND type = :type " +
            "ORDER BY timestamp DESC")
    fun getMeasurementsOfPlantLiveData(plantId: Long, type: Int): LiveData< List<PlantMeasurement> >

    @Query("SELECT * FROM plantMeasurements WHERE plantId = :plantId AND type = :type " +
            "ORDER BY timestamp DESC LIMIT 1")
    fun getLastMeasurementOfPlant(plantId: Long, type: Int): PlantMeasurement?

    @Query("SELECT * FROM plantMeasurements WHERE plantId = :plantId AND type = :type " +
            "ORDER BY timestamp DESC LIMIT 1")
    fun getLastMeasurementOfPlantLiveData(plantId: Long, type: Int): LiveData<PlantMeasurement?>
}