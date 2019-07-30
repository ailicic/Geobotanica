package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.GeolocationDao
import com.geobotanica.geobotanica.data.entity.Geolocation
import com.geobotanica.geobotanica.network.Geolocator
import com.geobotanica.geobotanica.network.Resource
import com.geobotanica.geobotanica.network.createNetworkBoundResource
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject


class GeolocationRepo @Inject constructor(
        private val geolocationDao: GeolocationDao,
        private val geolocator: Geolocator
) {
    fun insert(geolocation: Geolocation): Long = geolocationDao.insert(geolocation)

    fun get(id: Long): Geolocation = geolocationDao.get(id)
    fun getAll(): LiveData<List <Geolocation>> = geolocationDao.getAll()

    fun delete(geolocation: Geolocation) = geolocationDao.delete(geolocation)

    fun get(): LiveData<Resource<Geolocation>> = createNetworkBoundResource(
            loadFromDb = { geolocationDao.getNewest() },
            shouldFetch = { it == null || it.timestamp.plusDays(1) < OffsetDateTime.now() },
            saveToDb = { geolocationDao.insert(it) },
            fetchData = { geolocator.get() }
    )
}