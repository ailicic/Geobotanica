package com.geobotanica.geobotanica.data_ro.repo

import com.geobotanica.geobotanica.data_ro.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_ro.dao.TaxonCompositeDao
import com.geobotanica.geobotanica.data_ro.dao.TaxonDao
import com.geobotanica.geobotanica.data_ro.entity.Taxon
import com.geobotanica.geobotanica.data_ro.entity.TaxonComposite
import javax.inject.Inject


class TaxonRepo @Inject constructor(
        private val taxonDao: TaxonDao,
        private val taxonCompositeDao: TaxonCompositeDao
) {

    fun get(id: Long): Taxon? = taxonDao.get(id)

    fun genericStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): Set<Long>? =
            taxonDao.genericStartsWith(string, limit)?.toSet()

    fun epithetStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): Set<Long>? =
            taxonDao.epithetStartsWith(string, limit)?.toSet()

    fun genericOrEpithetStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): Set<Long>? =
            taxonDao.genericOrEpithetStartsWith(first, second, limit)?.toSet()

    fun getCount(): Int = taxonDao.getCount()

    fun getComposite(taxonId: Long): TaxonComposite = taxonCompositeDao.get(taxonId)

    fun getComposites(taxonIds: List<Long>): List<TaxonComposite> = taxonCompositeDao.get(taxonIds)
}