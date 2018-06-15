package com.geobotanica.geobotanica.data.repo

import com.geobotanica.geobotanica.data.dao.PlantDao
import com.geobotanica.geobotanica.data.entity.Plant
import javax.inject.Inject


class PlantRepo @Inject constructor(val plantDao: PlantDao) {
    fun get(id: Int): Plant = plantDao.get(id)

    fun getAll(): List<Plant> = plantDao.getAll()

    fun save(plant: Plant) {
        if (plant.id == 0L)
            plantDao.insert(plant)
        else
            plantDao.update(plant)
    }
}