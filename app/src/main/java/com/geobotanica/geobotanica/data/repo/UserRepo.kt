package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.UserDao
import com.geobotanica.geobotanica.data.entity.User
import javax.inject.Inject


class UserRepo @Inject constructor(private val userDao: UserDao) {

    suspend fun insert(user: User): Long = userDao.insert(user)

    suspend fun get(id: Long): User = userDao.get(id)

    fun getLiveData(id: Long): LiveData<User> = userDao.getLiveData(id)

    suspend fun getAll(): List<User> = userDao.getAll()

//    fun getAllLiveData(): LiveData<List<User>> = userDao.getAllLiveData()

    suspend fun getByNickname(nickname: String): User = userDao.getByNickname(nickname)
}