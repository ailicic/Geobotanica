package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.Plant

@Dao
interface PlantDao : BaseDao<Plant> {
    @Query("SELECT * FROM plants WHERE id = :id")
    fun get(id: Long): LiveData<Plant>

//    @Query("SELECT * FROM plants")
//    fun getAll(): LiveData<List<Plant>>

    @Query("SELECT plants.* FROM plants WHERE plants.userId = :userId")
    fun getAllPlantsByUser(userId: Long): LiveData<List<Plant>>

    @Query("SELECT * FROM plants WHERE commonName = :commonName")
    fun getPlantsByCommonName(commonName: String): LiveData<List<Plant>>

    @Query("SELECT * FROM plants WHERE scientificName = :scientificName")
    fun getPlantsByScientificName(scientificName: String): LiveData<List<Plant>>
}