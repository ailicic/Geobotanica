package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.PlantMeasurementDao
import com.geobotanica.geobotanica.data.entity.PlantMeasurement
import javax.inject.Inject


class PlantMeasurementRepo @Inject constructor(private val measurementDao: PlantMeasurementDao) {

    suspend fun insert(plantMeasurement: PlantMeasurement): Long = measurementDao.insert(plantMeasurement)

    fun get(id: Long): LiveData<PlantMeasurement> = measurementDao.get(id)

    fun getHeightOfPlant(plantId: Long): LiveData<PlantMeasurement?> =
            measurementDao.getMeasurementOfPlant(plantId, PlantMeasurement.Type.HEIGHT.ordinal)

    fun getDiameterOfPlant(plantId: Long): LiveData<PlantMeasurement?> =
            measurementDao.getMeasurementOfPlant(plantId, PlantMeasurement.Type.DIAMETER.ordinal)

    fun getTrunkDiameterOfPlant(plantId: Long): LiveData<PlantMeasurement?> =
            measurementDao.getMeasurementOfPlant(plantId, PlantMeasurement.Type.TRUNK_DIAMETER.ordinal)

    fun getHeightsOfPlant(plantId: Long): LiveData< List<PlantMeasurement> > =
            measurementDao.getMeasurementsOfPlant(plantId, PlantMeasurement.Type.HEIGHT.ordinal)

    fun getDiametersOfPlant(plantId: Long): LiveData< List<PlantMeasurement> >  =
            measurementDao.getMeasurementsOfPlant(plantId, PlantMeasurement.Type.DIAMETER.ordinal)

    fun getTrunkDiametersOfPlant(plantId: Long): LiveData< List<PlantMeasurement> >  =
            measurementDao.getMeasurementsOfPlant(plantId, PlantMeasurement.Type.TRUNK_DIAMETER.ordinal)

    fun getAllMeasurementsOfPlant(plantId: Long): LiveData<List<PlantMeasurement>> =
            measurementDao.getAllMeasurementsOfPlant(plantId)
}