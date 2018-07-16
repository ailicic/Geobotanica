package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.MeasurementDao
import com.geobotanica.geobotanica.data.entity.Measurement
import javax.inject.Inject


class MeasurementRepo @Inject constructor(private val measurementDao: MeasurementDao) {
    fun get(id: Long): LiveData<Measurement> = measurementDao.get(id)

//    fun getAllMeasurementsOfPlant(plantId: Long): LiveData<List<Measurement>> = measurementDao.getAllMeasurementsOfPlant(plantId)

    fun getHeightOfPlant(plantId: Long): LiveData<Measurement> =
            measurementDao.getMeasurementOfPlant(plantId, Measurement.Type.HEIGHT.ordinal)

    fun getDiameterOfPlant(plantId: Long): LiveData<Measurement> =
            measurementDao.getMeasurementOfPlant(plantId, Measurement.Type.DIAMETER.ordinal)

    fun getTrunkDiameterOfPlant(plantId: Long): LiveData<Measurement> =
            measurementDao.getMeasurementOfPlant(plantId, Measurement.Type.TRUNK_DIAMETER.ordinal)

    fun insert(measurement: Measurement): Long = measurementDao.insert(measurement)
}