package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.PlantMeasurementDao
import com.geobotanica.geobotanica.data.entity.PlantMeasurement
import javax.inject.Inject


class PlantMeasurementRepo @Inject constructor(private val measurementDao: PlantMeasurementDao) {

    suspend fun insert(plantMeasurement: PlantMeasurement): Long = measurementDao.insert(plantMeasurement)

    suspend fun delete(vararg measurement: PlantMeasurement) = measurementDao.delete(*measurement)

    suspend fun get(id: Long): PlantMeasurement = measurementDao.get(id)

    suspend fun getAllOfPlant(
            plantId: Long,
            typeFlags: Int = PlantMeasurement.Type.ALL.flag
    ): List<PlantMeasurement> =
            measurementDao.getAllOfPlant(plantId, typeFlags)

    fun getLastOfPlantLiveData(
            plantId: Long,
            typeFlags: Int = PlantMeasurement.Type.ALL.flag
    ): LiveData<PlantMeasurement?> =
            measurementDao.getLastOfPlantLiveData(plantId, typeFlags)
}