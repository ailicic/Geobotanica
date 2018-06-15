package com.geobotanica.geobotanica.data.repo

import com.geobotanica.geobotanica.data.dao.LocationDao
import com.geobotanica.geobotanica.data.entity.Location
import javax.inject.Inject

class LocationRepo @Inject constructor(val locationDao: LocationDao) {
    fun get(id: Int): Location = locationDao.get(id)

    fun save(location: Location) {
        if (location.id == 0L)
            locationDao.insert(location)
        else
            locationDao.update(location)
    }
}