package com.geobotanica.geobotanica.data.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import com.geobotanica.geobotanica.data.entity.Photo

@Dao
interface PhotoDao : BaseDao<Photo> {
    @Query("SELECT * FROM photos WHERE id=:id")
    fun get(id: Long): Photo

    @Query("SELECT * FROM photos WHERE plantId=:plantId ORDER BY timestamp DESC")
    fun getAllPhotosOfPlant(plantId: Long): List<Photo>

    @Query("SELECT * FROM photos WHERE userId=:userId")
    fun getAllPhotosByUser(userId: Long): List<Photo>
}