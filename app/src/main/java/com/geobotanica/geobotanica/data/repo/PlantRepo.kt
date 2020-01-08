package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.PlantCompositeDao
import com.geobotanica.geobotanica.data.dao.PlantDao
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.PlantComposite
import javax.inject.Inject


class PlantRepo @Inject constructor(
        private val plantDao: PlantDao,
        private val plantCompositeDao: PlantCompositeDao
) {

    suspend fun insert(plant: Plant): Long = plantDao.insert(plant)

    suspend fun update(plant: Plant): Int = plantDao.update(plant)

    suspend fun delete(plant: Plant) = plantDao.delete(plant)

    suspend fun get(id: Long): Plant = plantDao.get(id)

    fun getLiveData(id: Long): LiveData<Plant> = plantDao.getLiveData(id)

//    fun getAll(): LiveData<List<Plant>> = plantDao.getAll()

//    fun getPlantComposite(plantId: Long): LiveData<PlantComposite> = plantCompositeDao.get(plantId)

    fun getAllPlantComposites(): LiveData<List<PlantComposite>> = plantCompositeDao.getAll()
}