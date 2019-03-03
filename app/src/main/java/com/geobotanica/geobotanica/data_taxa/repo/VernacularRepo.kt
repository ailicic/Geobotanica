package com.geobotanica.geobotanica.data_taxa.repo

import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.dao.VernacularDao
import com.geobotanica.geobotanica.data_taxa.dao.VernacularStarDao
import com.geobotanica.geobotanica.data_taxa.entity.Vernacular
import javax.inject.Inject


class VernacularRepo @Inject constructor(
        private val vernacularDao: VernacularDao,
        private val vernacularStarDao: VernacularStarDao
) {

    fun get(id: Long): Vernacular? = vernacularDao.get(id)

    fun nonFirstWordStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            vernacularDao.nonFirstWordStartsWith(string, limit)

    fun firstWordStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            vernacularDao.firstWordStartsWith(string, limit)

    fun anyWordStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            vernacularDao.anyWordStartsWith(first, second, limit)

//    fun contains(string: String): List<Long>? = vernacularDao.contains(string)

    fun getCount(): Int = vernacularDao.getCount()

    // VernacularStarDao

    fun getAllStarred(): List<Long> = vernacularStarDao.getAll() ?: emptyList()

    fun setStarred(id: Long, isStarred: Boolean) =
            if (isStarred) vernacularStarDao.setStarred(id) else vernacularStarDao.unsetStarred(id)

    fun starredStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            vernacularStarDao.starredStartsWith(string, limit)

    fun starredStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            vernacularStarDao.starredStartsWith(first, second, limit)
}