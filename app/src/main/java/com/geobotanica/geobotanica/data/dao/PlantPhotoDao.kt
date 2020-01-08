package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.PlantPhoto

@Dao
interface PlantPhotoDao : BaseDao<PlantPhoto> {
    @Query("SELECT * FROM plantPhotos WHERE id = :id")
    fun get(id: Long): LiveData<PlantPhoto>

    @Query("SELECT * FROM plantPhotos WHERE plantId = :plantId ORDER BY timestamp ASC")
    fun getAllPhotosOfPlantLiveData(plantId: Long): LiveData<List<PlantPhoto>>

    @Query("SELECT * FROM plantPhotos WHERE plantId = :plantId ORDER BY timestamp ASC")
    suspend fun getAllPhotosOfPlant(plantId: Long): List<PlantPhoto>

//    @Query("SELECT * FROM plantPhotos WHERE userId = :userId")
//    suspend fun getAllPhotosByUser(userId: Long): List<PlantPhoto>
}