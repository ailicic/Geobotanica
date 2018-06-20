package com.geobotanica.geobotanica.data.repo

import com.geobotanica.geobotanica.data.dao.LocationDao
import com.geobotanica.geobotanica.data.entity.Location
import javax.inject.Inject

class LocationRepo @Inject constructor(val locationDao: LocationDao) {
    fun insert(location: Location): Long = locationDao.insert(location)

    fun get(id: Long): Location = locationDao.get(id)

    fun getPlantLocation(plantId: Long) = locationDao.getPlantLocation(plantId)
}