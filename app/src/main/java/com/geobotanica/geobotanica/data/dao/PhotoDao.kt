package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.Photo

@Dao
interface PhotoDao : BaseDao<Photo> {
    @Query("SELECT * FROM photos WHERE id = :id")
    fun get(id: Long): LiveData<Photo>

    @Query("SELECT * FROM photos WHERE plantId = :plantId ORDER BY timestamp DESC") // TODO: Check ordering
    fun getAllPhotosOfPlant(plantId: Long): LiveData<List<Photo>>

    @Query("SELECT * FROM photos WHERE plantId = :plantId AND type = :type ORDER BY timestamp DESC LIMIT 1")
    fun getMainPhotoOfPlant(plantId: Long, type: Photo.Type): LiveData<Photo>

    @Query("SELECT * FROM photos WHERE userId = :userId")
    fun getAllPhotosByUser(userId: Long): List<Photo>
}