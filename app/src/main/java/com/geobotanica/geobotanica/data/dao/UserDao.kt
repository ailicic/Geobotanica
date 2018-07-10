package com.geobotanica.geobotanica.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import com.geobotanica.geobotanica.data.entity.User


@Dao
interface UserDao : BaseDao<User> {
    @Query("SELECT * FROM users WHERE id=:id")
    fun get(id: Long): LiveData<User>

    @Query("SELECT * FROM users")
    fun getAll(): LiveData<List<User>>

    @Query( "SELECT * FROM users WHERE nickname=:nickname")
    fun getByNickname(nickname: String): List<User>
}


//@Query("SELECT * FROM users WHERE id=:id")
//fun get(id: Long): Single<User>
//
////    @Query("SELECT * FROM users")
////    fun getAll(): Single<List<User>>
//
//@Query( "SELECT * FROM users WHERE nickname=:nickname")
//fun getByNickname(nickname: String): Single<List<User>>