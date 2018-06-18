package com.geobotanica.geobotanica.data.repo

import com.geobotanica.geobotanica.data.dao.LocationDao
import com.geobotanica.geobotanica.data.entity.Location
import javax.inject.Inject

class LocationRepo @Inject constructor(val locationDao: LocationDao) {
    fun get(id: Long): Location = locationDao.get(id)

    fun insert(location: Location): Long = locationDao.insert(location)
}