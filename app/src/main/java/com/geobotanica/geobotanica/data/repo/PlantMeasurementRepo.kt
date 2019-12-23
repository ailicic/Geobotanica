package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.PlantMeasurementDao
import com.geobotanica.geobotanica.data.entity.PlantMeasurement
import javax.inject.Inject


class PlantMeasurementRepo @Inject constructor(private val measurementDao: PlantMeasurementDao) {

    suspend fun insert(plantMeasurement: PlantMeasurement): Long = measurementDao.insert(plantMeasurement)

    suspend fun delete(vararg measurement: PlantMeasurement) = measurementDao.delete(*measurement)

    fun getLiveData(id: Long): LiveData<PlantMeasurement> = measurementDao.get(id)

    fun getLastHeightOfPlantLiveData(plantId: Long): LiveData<PlantMeasurement?> =
            measurementDao.getLastMeasurementOfPlantLiveData(plantId, PlantMeasurement.Type.HEIGHT.ordinal)

    fun getLastDiameterOfPlantLiveData(plantId: Long): LiveData<PlantMeasurement?> =
            measurementDao.getLastMeasurementOfPlantLiveData(plantId, PlantMeasurement.Type.DIAMETER.ordinal)

    suspend fun getLastTrunkDiameterOfPlant(plantId: Long): PlantMeasurement? =
            measurementDao.getLastMeasurementOfPlant(plantId, PlantMeasurement.Type.TRUNK_DIAMETER.ordinal)

    fun getLastTrunkDiameterOfPlantLiveData(plantId: Long): LiveData<PlantMeasurement?> =
            measurementDao.getLastMeasurementOfPlantLiveData(plantId, PlantMeasurement.Type.TRUNK_DIAMETER.ordinal)

    fun getHeightsOfPlantLiveData(plantId: Long): LiveData< List<PlantMeasurement> > =
            measurementDao.getMeasurementsOfPlantLiveData(plantId, PlantMeasurement.Type.HEIGHT.ordinal)

    fun getDiametersOfPlantLiveData(plantId: Long): LiveData< List<PlantMeasurement> >  =
            measurementDao.getMeasurementsOfPlantLiveData(plantId, PlantMeasurement.Type.DIAMETER.ordinal)

    suspend fun getTrunkDiametersOfPlant(plantId: Long): List<PlantMeasurement>  =
            measurementDao.getMeasurementsOfPlant(plantId, PlantMeasurement.Type.TRUNK_DIAMETER.ordinal)

    fun getTrunkDiametersOfPlantLiveData(plantId: Long): LiveData< List<PlantMeasurement> >  =
            measurementDao.getMeasurementsOfPlantLiveData(plantId, PlantMeasurement.Type.TRUNK_DIAMETER.ordinal)

    fun getLastMeasurementOfPlant(plantId: Long): LiveData<PlantMeasurement?> =
            measurementDao.getLastMeasurementOfPlantLiveData(plantId)
}