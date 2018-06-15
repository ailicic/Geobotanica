package com.geobotanica.geobotanica.data.dao

import android.arch.persistence.room.*

@Dao
interface BaseDao<T> {
    @Insert fun insert(vararg obj: T)
    @Delete fun delete(vararg obj: T)
    @Update fun update(vararg obj: T)

//    @Query fun get(id: Int): T
//    @Query fun getAll(): List<T>
}