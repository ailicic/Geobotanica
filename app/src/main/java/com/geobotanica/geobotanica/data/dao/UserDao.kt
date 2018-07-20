package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.User


@Dao
interface UserDao : BaseDao<User> {
    @Query("SELECT * FROM users WHERE id = :id")
    fun get(id: Long): LiveData<User>

    @Query("SELECT * FROM users")
    fun getAll(): LiveData<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) // TODO: Remove this after Login screen
    override fun insert(user:User): Long

//    @Query( "SELECT * FROM users WHERE nickname = :nickname" LIMIT 1)
//    fun getByNickname(nickname: String): User
}


//@Query("SELECT * FROM users WHERE id=:id")
//fun get(id: Long): Single<User>
//
////    @Query("SELECT * FROM users")
////    fun getAll(): Single<List<User>>
//
//@Query( "SELECT * FROM users WHERE nickname=:nickname")
//fun getByNickname(nickname: String): Single<List<User>>