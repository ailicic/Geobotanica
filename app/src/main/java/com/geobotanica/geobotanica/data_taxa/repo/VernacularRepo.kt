package com.geobotanica.geobotanica.data_taxa.repo

import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.dao.TagDao
import com.geobotanica.geobotanica.data_taxa.dao.TypeDao
import com.geobotanica.geobotanica.data_taxa.dao.VernacularDao
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.entity.Tag
import com.geobotanica.geobotanica.data_taxa.entity.Vernacular
import javax.inject.Inject


class VernacularRepo @Inject constructor(
        private val vernacularDao: VernacularDao,
        private val tagDao: TagDao,
        private val typeDao: TypeDao
) {

    suspend fun get(id: Long): Vernacular? = vernacularDao.get(id)

    suspend fun nonFirstWordStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            vernacularDao.nonFirstWordStartsWith(string, limit)

    suspend fun firstWordStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            vernacularDao.firstWordStartsWith(string, limit)

    suspend fun anyWordStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            vernacularDao.anyWordStartsWith(first, second, limit)

    suspend fun fromTaxonId(taxonId: Int): List<Long>? = vernacularDao.fromTaxonId(taxonId.toLong())

    suspend fun getCount(): Int = vernacularDao.getCount()

//    fun contains(string: String): List<Long>? = vernacularDao.contains(string)


    // Tagged Vernaculars

    suspend fun getAllStarred(limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.getAllVernacularsWithTag(STARRED.ordinal, limit)

    suspend fun getAllUsed(limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.getAllVernacularsWithTag(USED.ordinal, limit)

    suspend fun setTagged(id: Long, tag: PlantNameTag, isTagged: Boolean = true) {
        if (isTagged) {
            tagDao.getVernacularWithTag(id, tag.ordinal)?.let {
                updateTagTimestamp(it.vernacularId!!, tag)
            } ?: tagDao.insert(Tag(tag.ordinal, vernacularId = id))
        } else
            tagDao.unsetVernacularTag(id, tag.ordinal)
    }

    suspend fun updateTagTimestamp(id: Long, tag: PlantNameTag) = tagDao.updateVernacularTimestamp(id, tag.ordinal)

    suspend fun starredStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.taggedVernacularStartsWith(string, STARRED.ordinal, limit)

    suspend fun starredStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.taggedVernacularStartsWith(first, second, STARRED.ordinal, limit)

    suspend fun usedStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.taggedVernacularStartsWith(string, USED.ordinal, limit)

    suspend fun usedStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>? =
            tagDao.taggedVernacularStartsWith(first, second, USED.ordinal, limit)

    suspend fun starredFromTaxonId(taxonId: Int): List<Long>? =
            tagDao.taggedVernacularFromTaxonId(taxonId.toLong(), STARRED.ordinal)

    suspend fun usedFromTaxonId(taxonId: Int): List<Long>? =
            tagDao.taggedVernacularFromTaxonId(taxonId.toLong(), USED.ordinal)


    // Plant Types

    suspend fun getTypes(id: Long): Int = typeDao.getVernacularType(id).fold(0) { acc, it -> acc or it }

    suspend fun getTypeCount(): Int = typeDao.getVernacularCount()
}