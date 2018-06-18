package com.geobotanica.geobotanica.data.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.provider.ContactsContract
import com.geobotanica.geobotanica.data.entity.User


@Dao
interface UserDao : BaseDao<User> {
    @Query("SELECT * FROM users WHERE id=:id")
    fun get(id: Long): User

    @Query("SELECT * FROM users")
    fun getAll(): List<User>

    @Query( "SELECT * FROM users WHERE nickname=:nickname")
    fun getByNickname(nickname: String): List<User>
}