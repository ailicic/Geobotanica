package com.geobotanica.geobotanica.data.dao

import androidx.room.*

@Dao
interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(obj: T): Long
//    @Update fun update(obj: T)
//    @Delete fun delete(obj: T)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg obj: T): LongArray
    @Update fun update(vararg obj: T): Int // Returns number of rows updated
    @Delete fun delete(vararg obj: T): Int // Returns number of rows deleted
}