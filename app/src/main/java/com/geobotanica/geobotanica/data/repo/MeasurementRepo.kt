package com.geobotanica.geobotanica.data.repo

import com.geobotanica.geobotanica.data.dao.MeasurementDao
import com.geobotanica.geobotanica.data.entity.Measurement
import javax.inject.Inject


class MeasurementRepo @Inject constructor(val measurementDao: MeasurementDao) {
    fun get(id: Long): Measurement = measurementDao.get(id)

    fun getAllMeasurementsOfPlant(plantId: Long): List<Measurement> = measurementDao.getAllMeasurementsOfPlant(plantId)

    fun insert(measurement: Measurement): Long = measurementDao.insert(measurement)
}