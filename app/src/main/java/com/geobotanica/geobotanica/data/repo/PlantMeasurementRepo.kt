package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.PlantMeasurementDao
import com.geobotanica.geobotanica.data.entity.PlantMeasurement
import javax.inject.Inject


class PlantMeasurementRepo @Inject constructor(private val measurementDao: PlantMeasurementDao) {

    suspend fun insert(plantMeasurement: PlantMeasurement): Long = measurementDao.insert(plantMeasurement)

    suspend fun delete(vararg measurement: PlantMeasurement) = measurementDao.delete(*measurement)

    fun get(id: Long): LiveData<PlantMeasurement> = measurementDao.get(id)

    fun getLastHeightOfPlant(plantId: Long): LiveData<PlantMeasurement?> =
            measurementDao.getLastMeasurementOfPlantLiveData(plantId, PlantMeasurement.Type.HEIGHT.ordinal)

    fun getLastDiameterOfPlant(plantId: Long): LiveData<PlantMeasurement?> =
            measurementDao.getLastMeasurementOfPlantLiveData(plantId, PlantMeasurement.Type.DIAMETER.ordinal)

    fun getLastTrunkDiameterOfPlant(plantId: Long): PlantMeasurement? =
            measurementDao.getLastMeasurementOfPlant(plantId, PlantMeasurement.Type.TRUNK_DIAMETER.ordinal)

    fun getLastTrunkDiameterOfPlantLiveData(plantId: Long): LiveData<PlantMeasurement?> =
            measurementDao.getLastMeasurementOfPlantLiveData(plantId, PlantMeasurement.Type.TRUNK_DIAMETER.ordinal)

    fun getHeightsOfPlant(plantId: Long): LiveData< List<PlantMeasurement> > =
            measurementDao.getMeasurementsOfPlantLiveData(plantId, PlantMeasurement.Type.HEIGHT.ordinal)

    fun getDiametersOfPlant(plantId: Long): LiveData< List<PlantMeasurement> >  =
            measurementDao.getMeasurementsOfPlantLiveData(plantId, PlantMeasurement.Type.DIAMETER.ordinal)

    fun getTrunkDiametersOfPlant(plantId: Long): List<PlantMeasurement>  =
            measurementDao.getMeasurementsOfPlant(plantId, PlantMeasurement.Type.TRUNK_DIAMETER.ordinal)

    fun getTrunkDiametersOfPlantLiveData(plantId: Long): LiveData< List<PlantMeasurement> >  =
            measurementDao.getMeasurementsOfPlantLiveData(plantId, PlantMeasurement.Type.TRUNK_DIAMETER.ordinal)

    fun getAllMeasurementsOfPlant(plantId: Long): LiveData<List<PlantMeasurement>> =
            measurementDao.getAllMeasurementsOfPlantLiveData(plantId)
}