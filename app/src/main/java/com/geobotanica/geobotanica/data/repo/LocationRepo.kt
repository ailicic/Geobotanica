package com.geobotanica.geobotanica.data.repo

import android.arch.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.LocationDao
import com.geobotanica.geobotanica.data.entity.Location
import javax.inject.Inject

class LocationRepo @Inject constructor(private val locationDao: LocationDao) {
    fun insert(location: Location): Long = locationDao.insert(location)

    fun get(id: Long): LiveData<Location> = locationDao.get(id)

    fun getPlantLocation(plantId: Long): LiveData<Location> = locationDao.getPlantLocation(plantId)
}