package com.geobotanica.geobotanica.data.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import com.geobotanica.geobotanica.data.entity.Measurement

@Dao
interface MeasurementDao : BaseDao<Measurement> {
    @Query("SELECT * FROM measurements WHERE id=:id")
    fun get(id: Long): Measurement

    @Query("SELECT * FROM measurements WHERE plantId=:plantId")
    fun getAllMeasurementsOfPlant(plantId: Long): List<Measurement>
}