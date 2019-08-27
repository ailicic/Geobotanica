package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.PlantPhoto

@Dao
interface PlantPhotoDao : BaseDao<PlantPhoto> {
    @Query("SELECT * FROM plantPhotos WHERE id = :id")
    fun get(id: Long): LiveData<PlantPhoto>

    @Query("SELECT * FROM plantPhotos WHERE plantId = :plantId ORDER BY timestamp DESC") // TODO: Check ordering
    fun getAllPhotosOfPlantLiveData(plantId: Long): LiveData<List<PlantPhoto>>

    @Query("SELECT * FROM plantPhotos WHERE plantId = :plantId ORDER BY timestamp DESC") // TODO: Check ordering
    suspend fun getAllPhotosOfPlant(plantId: Long): List<PlantPhoto>

    @Query("SELECT * FROM plantPhotos WHERE plantId = :plantId AND type = :type ORDER BY timestamp DESC LIMIT 1")
    fun getMainPhotoOfPlant(plantId: Long, type: PlantPhoto.Type): LiveData<PlantPhoto>

    @Query("SELECT * FROM plantPhotos WHERE userId = :userId")
    suspend fun getAllPhotosByUser(userId: Long): List<PlantPhoto>
}