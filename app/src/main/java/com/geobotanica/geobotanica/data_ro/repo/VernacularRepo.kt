package com.geobotanica.geobotanica.data_ro.repo

import com.geobotanica.geobotanica.data_ro.dao.VernacularDao
import com.geobotanica.geobotanica.data_ro.entity.Vernacular
import javax.inject.Inject


class VernacularRepo @Inject constructor(private val vernacularDao: VernacularDao) {

    fun get(id: Long): Vernacular? = vernacularDao.get(id)

    fun startsWith(string: String): List<String>? = vernacularDao.startsWith(string.toLowerCase())

    fun secondWordStartsWith(string: String): List<String>? =
            vernacularDao.secondWordStartsWith(string.toLowerCase())

    fun contains(string: String): List<String>? = vernacularDao.contains("%$string%")

    fun getCount(): Int = vernacularDao.getCount()
}