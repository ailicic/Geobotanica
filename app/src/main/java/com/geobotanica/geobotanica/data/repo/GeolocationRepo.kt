package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.GeolocationDao
import com.geobotanica.geobotanica.data.entity.Geolocation
import com.geobotanica.geobotanica.network.Geolocator
import com.geobotanica.geobotanica.network.Resource
import com.geobotanica.geobotanica.network.createNetworkBoundResource
import com.geobotanica.geobotanica.util.GbTime
import org.threeten.bp.Duration
import javax.inject.Inject


class GeolocationRepo @Inject constructor(
        private val geolocationDao: GeolocationDao,
        private val geolocator: Geolocator
) {
    suspend fun insert(geolocation: Geolocation): Long = geolocationDao.insert(geolocation)

    suspend fun get(id: Long): Geolocation = geolocationDao.get(id)
    fun getAll(): LiveData<List <Geolocation>> = geolocationDao.getAll()

    suspend fun delete(geolocation: Geolocation) = geolocationDao.delete(geolocation)

    fun get(): LiveData<Resource<Geolocation>> = createNetworkBoundResource(
            loadFromDb = { geolocationDao.getNewest() },
            shouldFetch = { it == null || it.timestamp.plus(Duration.ofDays(1)) < GbTime.now() },
            saveToDb = { geolocationDao.insert(it) },
            fetchData = { geolocator.get() }
    )
}