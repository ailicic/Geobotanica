package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.Geolocation

@Dao
interface GeolocationDao : BaseDao<Geolocation> {
    @Query("SELECT COUNT(*) FROM geolocations")
    suspend fun count(): Int

    @Query("SELECT * FROM geolocations WHERE id = :id")
    suspend fun get(id: Long): Geolocation

    @Query("SELECT * FROM geolocations")
    fun getAll(): LiveData<List<Geolocation>>

    @Query("SELECT * FROM geolocations ORDER BY timestamp DESC LIMIT 1")
    suspend fun getNewest(): Geolocation?
}
