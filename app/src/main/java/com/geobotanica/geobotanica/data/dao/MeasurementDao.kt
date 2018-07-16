package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.Measurement

@Dao
interface MeasurementDao : BaseDao<Measurement> {
    @Query("SELECT * FROM measurements WHERE id = :id")
    fun get(id: Long): LiveData<Measurement>

//    @Query("SELECT * FROM measurements WHERE plantId = :plantId ORDER BY type ASC")
//    fun getAllMeasurementsOfPlant(plantId: Long): LiveData<List<Measurement>>

    @Query("SELECT * FROM measurements WHERE plantId = :plantId AND type = :type ORDER BY type ASC")
    fun getMeasurementOfPlant(plantId: Long, type: Int): LiveData<Measurement>
}