package com.geobotanica.geobotanica.data_taxa.repo

import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.dao.TaxonDao
import com.geobotanica.geobotanica.data_taxa.dao.StarredTaxonDao
import com.geobotanica.geobotanica.data_taxa.dao.UsedTaxonDao
import com.geobotanica.geobotanica.data_taxa.entity.Taxon
import javax.inject.Inject


class TaxonRepo @Inject constructor(
        private val taxonDao: TaxonDao,
        private val starredTaxonDao: StarredTaxonDao,
        private val usedTaxonDao: UsedTaxonDao
) {

    fun get(id: Long): Taxon? = taxonDao.get(id)

    fun genericStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            taxonDao.genericStartsWith(string, limit)

    fun epithetStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            taxonDao.epithetStartsWith(string, limit)

    fun genericOrEpithetStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            taxonDao.genericOrEpithetStartsWith(first, second, limit)

    fun getCount(): Int = taxonDao.getCount()

    // StarredTaxonDao

    fun getAllStarred(): List<Long> = starredTaxonDao.getAll() ?: emptyList()

    fun setStarred(id: Long, isStarred: Boolean) =
        if (isStarred) starredTaxonDao.setStarred(id) else starredTaxonDao.unsetStarred(id)

    fun starredStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
        starredTaxonDao.starredStartsWith(string, limit)

    fun starredStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
        starredTaxonDao.starredStartsWith(first, second, limit)

    // UsedTaxonDao

    fun getAllUsed(): List<Long> = usedTaxonDao.getAll() ?: emptyList()

    fun setUsed(id: Long, isUsed: Boolean) =
            if (isUsed) usedTaxonDao.setUsed(id) else usedTaxonDao.unsetUsed(id)

    fun usedStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            usedTaxonDao.usedStartsWith(string, limit)

    fun usedStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            usedTaxonDao.usedStartsWith(first, second, limit)
}