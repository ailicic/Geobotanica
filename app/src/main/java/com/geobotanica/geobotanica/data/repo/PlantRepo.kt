package com.geobotanica.geobotanica.data.repo

import android.arch.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.PlantDao
import com.geobotanica.geobotanica.data.entity.Plant
import javax.inject.Inject


class PlantRepo @Inject constructor(private val plantDao: PlantDao) {
    fun get(id: Long): LiveData<Plant> = plantDao.get(id)

    fun getAll(): LiveData<List<Plant>> = plantDao.getAll()

    fun insert(plant: Plant): Long = plantDao.insert(plant)

    fun delete(plant: Plant) = plantDao.delete(plant)
}