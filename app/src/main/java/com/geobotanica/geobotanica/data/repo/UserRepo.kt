package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.UserDao
import com.geobotanica.geobotanica.data.entity.User
import javax.inject.Inject


class UserRepo @Inject constructor(private val userDao: UserDao) {

    fun insert(user: User): Long = userDao.insert(user)

    fun get(id: Long): LiveData<User> = userDao.get(id)

    fun getAll(): LiveData<List<User>> = userDao.getAll()

//    fun getByNickname(nickname: String): LiveData<User> = userDao.getByNickname(nickname)

//    fun contains(nickname: String): Boolean {
//        return !userDao.getByNickname(nickname).isEmpty()
//    }
}