package com.geobotanica.geobotanica.data_taxa.repo

import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.dao.TagDao
import com.geobotanica.geobotanica.data_taxa.dao.VernacularDao
import com.geobotanica.geobotanica.data_taxa.entity.Tag
import com.geobotanica.geobotanica.data_taxa.entity.Vernacular
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag.*
import javax.inject.Inject


class VernacularRepo @Inject constructor(
        private val vernacularDao: VernacularDao,
        private val tagDao: TagDao
) {

    fun get(id: Long): Vernacular? = vernacularDao.get(id)

    fun nonFirstWordStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            vernacularDao.nonFirstWordStartsWith(string, limit)

    fun firstWordStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            vernacularDao.firstWordStartsWith(string, limit)

    fun anyWordStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            vernacularDao.anyWordStartsWith(first, second, limit)

    fun fromTaxonId(taxonId: Int): List<Long>? = vernacularDao.fromTaxonId(taxonId.toLong())

    fun getCount(): Int = vernacularDao.getCount()

//    fun contains(string: String): List<Long>? = vernacularDao.contains(string)


    // Tagged Vernaculars

    fun getAllStarred(limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.getAllVernacularsWithTag(STARRED.ordinal, limit)

    fun getAllUsed(limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.getAllVernacularsWithTag(USED.ordinal, limit)

    fun setTagged(id: Long, tag: PlantNameTag, isTagged: Boolean) {
        if (isTagged)
            tagDao.insert(Tag(tag.ordinal, vernacularId = id))
        else
            tagDao.unsetVernacularTag(id, tag.ordinal)
    }

    fun starredStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.taggedVernacularStartsWith(string, STARRED.ordinal, limit)

    fun starredStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.taggedVernacularStartsWith(first, second, STARRED.ordinal, limit)

    fun usedStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.taggedVernacularStartsWith(string, USED.ordinal, limit)

    fun usedStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.taggedVernacularStartsWith(first, second, USED.ordinal, limit)

    fun starredFromTaxonId(taxonId: Int): List<Long>? =
            tagDao.taggedVernacularFromTaxonId(taxonId.toLong(), STARRED.ordinal)

    fun usedFromTaxonId(taxonId: Int): List<Long>? =
            tagDao.taggedVernacularFromTaxonId(taxonId.toLong(), USED.ordinal)
}