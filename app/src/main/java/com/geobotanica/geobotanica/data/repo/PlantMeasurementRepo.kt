package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.PlantMeasurementDao
import com.geobotanica.geobotanica.data.entity.PlantMeasurement
import javax.inject.Inject


class PlantMeasurementRepo @Inject constructor(private val measurementDao: PlantMeasurementDao) {

    suspend fun insert(plantMeasurement: PlantMeasurement): Long = measurementDao.insert(plantMeasurement)

    suspend fun delete(vararg measurement: PlantMeasurement) = measurementDao.delete(*measurement)

    suspend fun getAllOfPlant(
            plantId: Long,
            typeFlags: Int = PlantMeasurement.Type.ALL.flag
    ): List<PlantMeasurement> =
            measurementDao.getAllOfPlant(plantId, typeFlags)

    fun getAllOfPlantLiveData(
            plantId: Long,
            typeFlags: Int = PlantMeasurement.Type.ALL.flag
    ): LiveData< List<PlantMeasurement> > =
            measurementDao.getAllOfPlantLiveData(plantId, typeFlags)

    suspend fun getLastOfPlant(plantId: Long, typeFlags: Int = PlantMeasurement.Type.ALL.flag): PlantMeasurement? =
            measurementDao.getLastOfPlant(plantId, typeFlags)

    fun getLastOfPlantLiveData(
            plantId: Long,
            typeFlags: Int = PlantMeasurement.Type.ALL.flag
    ): LiveData<PlantMeasurement?> =
            measurementDao.getLastOfPlantLiveData(plantId, typeFlags)
}