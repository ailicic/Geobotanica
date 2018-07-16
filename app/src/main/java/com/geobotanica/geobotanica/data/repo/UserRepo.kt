package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.geobotanica.geobotanica.data.dao.UserDao
import com.geobotanica.geobotanica.data.entity.User
import javax.inject.Inject


class UserRepo @Inject constructor(private val userDao: UserDao) {
    fun get(id: Long): LiveData<User> = userDao.get(id)

    fun getAll(): LiveData<List<User>> = userDao.getAll()

//    fun getByNickname(nickname: String): LiveData<User> = userDao.getByNickname(nickname)

    fun insert(user: User): Long = userDao.insert(user)

//    fun contains(nickname: String): Boolean {
//        return !userDao.getByNickname(nickname).isEmpty()
//    }
}


//fun get(id: Long): Single<User> = userDao.get(id)
//
////    fun getAll(): Single<List<User>> = userDao.getAll()
//
//fun getByNickname(nickname: String): Single<List<User>> = userDao.getByNickname(nickname)
//
//fun insert(user: User): Long = userDao.insert(user)