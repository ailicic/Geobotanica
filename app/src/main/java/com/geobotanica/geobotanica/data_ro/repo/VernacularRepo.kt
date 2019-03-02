package com.geobotanica.geobotanica.data_ro.repo

import com.geobotanica.geobotanica.data_ro.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_ro.dao.VernacularDao
import com.geobotanica.geobotanica.data_ro.entity.Vernacular
import javax.inject.Inject


class VernacularRepo @Inject constructor(private val vernacularDao: VernacularDao) {

    fun get(id: Long): Vernacular? = vernacularDao.get(id)

    fun nonFirstWordStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): Set<Long>? =
            vernacularDao.nonFirstWordStartsWith(string, limit)?.toSet()

    fun firstWordStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): Set<Long>? =
            vernacularDao.firstWordStartsWith(string, limit)?.toSet()

    fun anyWordStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): Set<Long>? =
            vernacularDao.anyWordStartsWith(first, second, limit)?.toSet()

//    fun contains(string: String): List<Long>? = vernacularDao.contains(string)

    fun getCount(): Int = vernacularDao.getCount()
}