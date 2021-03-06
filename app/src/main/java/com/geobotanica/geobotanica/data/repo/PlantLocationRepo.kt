package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.PlantLocationDao
import com.geobotanica.geobotanica.data.entity.PlantLocation
import javax.inject.Inject

class PlantLocationRepo @Inject constructor(private val plantLocationDao: PlantLocationDao) {
    suspend fun insert(plantLocation: PlantLocation): Long = plantLocationDao.insert(plantLocation)

    suspend fun get(id: Long): PlantLocation = plantLocationDao.get(id)

    fun getLiveData(id: Long): LiveData<PlantLocation> = plantLocationDao.getLiveData(id)

//    fun getPlantLocations(plantId: Long): LiveData<List<PlantLocation>> = plantLocationDao.getPlantLocations(plantId)

    fun getLastPlantLocation(plantId: Long): LiveData<PlantLocation> = plantLocationDao.getLastPlantLocation(plantId)
}