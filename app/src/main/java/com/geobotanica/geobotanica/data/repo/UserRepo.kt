package com.geobotanica.geobotanica.data.repo

import com.geobotanica.geobotanica.data.dao.UserDao
import com.geobotanica.geobotanica.data.entity.User
import javax.inject.Inject


class UserRepo @Inject constructor(val userDao: UserDao) {
    fun get(id: Int): User = userDao.get(id)

    fun getAll(): List<User> = userDao.getAll()

    fun save(user: User) {
        if (user.id == 0L)
            userDao.insert(user)
        else
            userDao.update(user)
    }

//    private fun contains(user: User): Boolean {
//        return !userDao.getByNickname(user.nickname).isEmpty()
//    }

}