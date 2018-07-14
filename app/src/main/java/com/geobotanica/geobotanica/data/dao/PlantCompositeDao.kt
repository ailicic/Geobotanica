package com.geobotanica.geobotanica.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction
import com.geobotanica.geobotanica.data.entity.PlantComposite


@Dao
interface PlantCompositeDao {
    @Transaction @Query("SELECT * from plants WHERE id = :id")
    fun get(id: Long): LiveData<PlantComposite>

    @Transaction @Query("SELECT * from plants")
    fun getAll(): LiveData<List<PlantComposite>>
}