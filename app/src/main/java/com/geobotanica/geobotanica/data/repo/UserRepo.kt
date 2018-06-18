package com.geobotanica.geobotanica.data.repo

import com.geobotanica.geobotanica.data.dao.UserDao
import com.geobotanica.geobotanica.data.entity.User
import javax.inject.Inject


class UserRepo @Inject constructor(val userDao: UserDao) {
    fun get(id: Long): User = userDao.get(id)

    fun getAll(): List<User> = userDao.getAll()

    fun getByNickname(nickname: String): List<User> = userDao.getByNickname(nickname)

    fun insert(user: User): Long = userDao.insert(user)

    fun contains(nickname: String): Boolean {
        return !userDao.getByNickname(nickname).isEmpty()
    }

}