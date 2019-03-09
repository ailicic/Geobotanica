package com.geobotanica.geobotanica.data_taxa.repo

import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.dao.VernacularDao
import com.geobotanica.geobotanica.data_taxa.dao.StarredVernacularDao
import com.geobotanica.geobotanica.data_taxa.dao.UsedVernacularDao
import com.geobotanica.geobotanica.data_taxa.entity.Vernacular
import javax.inject.Inject


class VernacularRepo @Inject constructor(
        private val vernacularDao: VernacularDao,
        private val starredVernacularDao: StarredVernacularDao,
        private val usedVernacularDao: UsedVernacularDao
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

    // StarredVernacularDao

    fun getAllStarred(): List<Long> = starredVernacularDao.getAll() ?: emptyList()

    fun setStarred(id: Long, isStarred: Boolean) =
            if (isStarred) starredVernacularDao.setStarred(id) else starredVernacularDao.unsetStarred(id)

    fun starredStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            starredVernacularDao.starredStartsWith(string, limit)

    fun starredStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            starredVernacularDao.starredStartsWith(first, second, limit)

    // UsedVernacularDao

    fun getAllUsed(): List<Long> = usedVernacularDao.getAll() ?: emptyList()

    fun setUsed(id: Long, isUsed: Boolean) =
            if (isUsed) usedVernacularDao.setUsed(id) else usedVernacularDao.unsetUsed(id)

    fun usedStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            usedVernacularDao.usedStartsWith(string, limit)

    fun usedStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            usedVernacularDao.usedStartsWith(first, second, limit)
}