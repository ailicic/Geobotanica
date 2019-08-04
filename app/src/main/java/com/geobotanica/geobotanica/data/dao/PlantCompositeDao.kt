package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.geobotanica.geobotanica.data.entity.PlantComposite


@Dao
interface PlantCompositeDao {

    @Transaction @Query("SELECT * FROM plants WHERE id = :plantId")
    fun get(plantId: Long): LiveData<PlantComposite>

    @Transaction @Query("SELECT * FROM plants")
    fun getAll(): LiveData<List<PlantComposite>>
}