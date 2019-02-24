package com.geobotanica.geobotanica.data_ro.repo

import com.geobotanica.geobotanica.data_ro.dao.TaxonDao
import com.geobotanica.geobotanica.data_ro.entity.Taxon
import javax.inject.Inject


class TaxonRepo @Inject constructor(private val taxaDao: TaxonDao) {

    fun get(id: Long): Taxon? = taxaDao.get(id)

    fun generalStartsWith(string: String): List<String>? = taxaDao.generalStartsWith(string.toLowerCase())

    fun epithetStartsWith(string: String): List<String>? = taxaDao.epithetStartsWith(string.toLowerCase())

    fun getCount(): Int = taxaDao.getCount()
}