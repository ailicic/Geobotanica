package com.geobotanica.geobotanica.data.dao

import androidx.room.*

@Dao
interface BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(obj: T): Long
//    @Update fun update(obj: T)
//    @Delete fun delete(obj: T)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg obj: T): LongArray
    @Update suspend fun update(vararg obj: T): Int // Returns number of rows updated
    @Delete suspend fun delete(vararg obj: T): Int // Returns number of rows deleted
}