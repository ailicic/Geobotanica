package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.User


@Dao
interface UserDao : BaseDao<User> {

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun get(id: Long): User

    @Query("SELECT * FROM users WHERE id = :id")
    fun getLiveData(id: Long): LiveData<User>

    @Query("SELECT * FROM users")
    fun getAll(): LiveData<List<User>>
}