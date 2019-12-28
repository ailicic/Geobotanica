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

    @Query("SELECT * FROM users ORDER BY nickname ASC")
    suspend fun getAll(): List<User>

    @Query("SELECT * FROM users ORDER BY nickname ASC")
    fun getAllLiveData(): LiveData<List<User>>

    @Query("SELECT * FROM users where nickname = :nickname")
    suspend fun getByNickname(nickname: String): User
}