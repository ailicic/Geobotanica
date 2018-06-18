package com.geobotanica.geobotanica.data.repo

import com.geobotanica.geobotanica.data.dao.PlantDao
import com.geobotanica.geobotanica.data.entity.Plant
import javax.inject.Inject


class PlantRepo @Inject constructor(val plantDao: PlantDao) {
    fun get(id: Long): Plant = plantDao.get(id)

    fun getAll(): List<Plant> = plantDao.getAll()

    fun insert(plant: Plant): Long = plantDao.insert(plant)
}