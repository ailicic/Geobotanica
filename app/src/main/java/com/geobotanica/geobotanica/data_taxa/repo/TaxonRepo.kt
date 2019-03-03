package com.geobotanica.geobotanica.data_taxa.repo

import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.dao.TaxonDao
import com.geobotanica.geobotanica.data_taxa.dao.TaxonStarDao
import com.geobotanica.geobotanica.data_taxa.entity.Taxon
import javax.inject.Inject


class TaxonRepo @Inject constructor(
        private val taxonDao: TaxonDao,
        private val taxonStarDao: TaxonStarDao
) {

    fun get(id: Long): Taxon? = taxonDao.get(id)

    fun genericStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            taxonDao.genericStartsWith(string, limit)

    fun epithetStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            taxonDao.epithetStartsWith(string, limit)

    fun genericOrEpithetStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            taxonDao.genericOrEpithetStartsWith(first, second, limit)

    fun getCount(): Int = taxonDao.getCount()

    // TaxonStarDao

    fun getAllStarred(): List<Long> = taxonStarDao.getAll() ?: emptyList()

    fun setStarred(id: Long, isStarred: Boolean) =
        if (isStarred) taxonStarDao.setStarred(id) else taxonStarDao.unsetStarred(id)

    fun starredStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
        taxonStarDao.starredStartsWith(string, limit)

    fun starredStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
        taxonStarDao.starredStartsWith(first, second, limit)
}