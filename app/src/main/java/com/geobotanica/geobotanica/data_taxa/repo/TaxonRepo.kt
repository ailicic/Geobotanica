package com.geobotanica.geobotanica.data_taxa.repo

import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.dao.TagDao
import com.geobotanica.geobotanica.data_taxa.dao.TaxonDao
import com.geobotanica.geobotanica.data_taxa.entity.Tag
import com.geobotanica.geobotanica.data_taxa.entity.Taxon
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag.STARRED
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag.USED
import javax.inject.Inject


class TaxonRepo @Inject constructor(
        private val taxonDao: TaxonDao,
        private val tagDao: TagDao
) {

    fun get(id: Long): Taxon? = taxonDao.get(id)

    fun genericStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            taxonDao.genericStartsWith(string, limit)

    fun epithetStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            taxonDao.epithetStartsWith(string, limit)

    fun genericOrEpithetStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            taxonDao.genericOrEpithetStartsWith(first, second, limit)

    fun fromVernacularId(vernacularId: Int): List<Long>? = taxonDao.fromVernacularId(vernacularId.toLong())

    fun getCount(): Int = taxonDao.getCount()


    // Tagged Taxa

    fun getAllStarred(limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.getAllTaxaWithTag(STARRED.ordinal, limit)

    fun getAllUsed(limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.getAllTaxaWithTag(USED.ordinal, limit)

    fun setTagged(id: Long, tag: PlantNameTag, isTagged: Boolean) {
        if (isTagged)
            tagDao.insert(Tag(tag.ordinal, taxonId = id))
        else
            tagDao.unsetTaxonTag(id, tag.ordinal)
    }

    fun starredStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.taggedTaxonStartsWith(string, STARRED.ordinal, limit)

    fun starredStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.taggedTaxonStartsWith(first, second, STARRED.ordinal, limit)

    fun usedStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.taggedTaxonStartsWith(string, USED.ordinal, limit)

    fun usedStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.taggedTaxonStartsWith(first, second, USED.ordinal, limit)

    fun starredFromVernacularId(vernacularId: Int): List<Long>? =
            tagDao.taggedTaxonFromVernacularId(vernacularId.toLong(), STARRED.ordinal)

    fun usedFromVernacularId(vernacularId: Int): List<Long>? =
            tagDao.taggedTaxonFromVernacularId(vernacularId.toLong(), USED.ordinal)
}