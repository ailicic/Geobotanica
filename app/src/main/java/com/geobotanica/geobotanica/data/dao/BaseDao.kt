package com.geobotanica.geobotanica.data.dao

import androidx.room.*

@Dao
interface BaseDao<T> {
    @Insert fun insert(obj: T): Long
    @Update fun update(obj: T): Int
    @Delete fun delete(obj: T)
    @Insert fun insert(vararg obj: T): LongArray
    @Update fun update(vararg obj: T): Int
    @Delete fun delete(vararg obj: T)
}