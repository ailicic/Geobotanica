package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.geobotanica.geobotanica.data.entity.PlantComposite


@Dao
interface PlantCompositeDao {
    @Transaction @Query("SELECT * from plants WHERE id = :id")
    fun get(id: Long): LiveData<PlantComposite>

    @Transaction @Query("SELECT * from plants")
    fun getAll(): LiveData<List<PlantComposite>>
}