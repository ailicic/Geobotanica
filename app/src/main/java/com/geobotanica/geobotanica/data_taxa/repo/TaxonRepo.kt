package com.geobotanica.geobotanica.data_taxa.repo

import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.dao.TaxonDao
import com.geobotanica.geobotanica.data_taxa.entity.Taxon
import javax.inject.Inject


class TaxonRepo @Inject constructor(private val taxonDao: TaxonDao) {

    fun get(id: Long): Taxon? = taxonDao.get(id)

    fun genericStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): Set<Long>? =
            taxonDao.genericStartsWith(string, limit)?.toSet()

    fun epithetStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): Set<Long>? =
            taxonDao.epithetStartsWith(string, limit)?.toSet()

    fun genericOrEpithetStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): Set<Long>? =
            taxonDao.genericOrEpithetStartsWith(first, second, limit)?.toSet()

    fun getCount(): Int = taxonDao.getCount()
}