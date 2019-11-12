package com.geobotanica.geobotanica.data_taxa.repo

import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data_taxa.DEFAULT_RESULT_LIMIT
import com.geobotanica.geobotanica.data_taxa.dao.TagDao
import com.geobotanica.geobotanica.data_taxa.dao.TaxonDao
import com.geobotanica.geobotanica.data_taxa.dao.TypeDao
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.entity.Tag
import com.geobotanica.geobotanica.data_taxa.entity.Taxon
import com.geobotanica.geobotanica.data_taxa.entity.TaxonType
import javax.inject.Inject


class TaxonRepo @Inject constructor(
        private val taxonDao: TaxonDao,
        private val tagDao: TagDao,
        private val typeDao: TypeDao
) {

    suspend fun get(id: Long): Taxon? = taxonDao.get(id)

//    fun getAllIds(): Cursor = taxonDao.getAllIds()

    suspend fun genericStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long> =
            taxonDao.genericStartsWith(string, limit)

    suspend fun epithetStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long> =
            taxonDao.epithetStartsWith(string, limit)

    suspend fun genericOrEpithetStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long> =
            taxonDao.genericOrEpithetStartsWith(first, second, limit)

    suspend fun fromVernacularId(vernacularId: Int): List<Long> = taxonDao.fromVernacularId(vernacularId.toLong())

    suspend fun getCount(): Int = taxonDao.getCount()


    // Tagged Taxa

    suspend fun getAllStarred(limit: Int = DEFAULT_RESULT_LIMIT): List<Long> =
            tagDao.getAllTaxaWithTag(STARRED.ordinal, limit)

    suspend fun getAllUsed(limit: Int = DEFAULT_RESULT_LIMIT): List<Long> =
            tagDao.getAllTaxaWithTag(USED.ordinal, limit)

    suspend fun setTagged(id: Long, tag: PlantNameTag, isTagged: Boolean = true) {
        if (isTagged) {
            tagDao.getTaxonWithTag(id, tag.ordinal)?.let {
                updateTagTimestamp(it.taxonId!!, tag)
            } ?: tagDao.insert(Tag(tag.ordinal, taxonId = id))
        } else
            tagDao.unsetTaxonTag(id, tag.ordinal)
    }

    suspend fun updateTagTimestamp(id: Long, tag: PlantNameTag) = tagDao.updateTaxonTimestamp(id, tag.ordinal)


    suspend fun starredStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long> =
            tagDao.taggedTaxonStartsWith(string, STARRED.ordinal, limit)

    suspend fun starredStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long> =
            tagDao.taggedTaxonStartsWith(first, second, STARRED.ordinal, limit)

    suspend fun usedStartsWith(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long> =
            tagDao.taggedTaxonStartsWith(string, USED.ordinal, limit)

    suspend fun usedStartsWith(first: String, second: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long> =
            tagDao.taggedTaxonStartsWith(first, second, USED.ordinal, limit)

    suspend fun starredFromVernacularId(vernacularId: Int): List<Long> =
            tagDao.taggedTaxonFromVernacularId(vernacularId.toLong(), STARRED.ordinal)

    suspend fun usedFromVernacularId(vernacularId: Int): List<Long> =
            tagDao.taggedTaxonFromVernacularId(vernacularId.toLong(), USED.ordinal)


    // Plant Types

    suspend fun insertType(obj: TaxonType): Long = typeDao.insert(obj)

    suspend fun getTypes(id: Long): Int {
        if (taxonDao.getKingdom(id) == Taxon.Kingdom.FUNGI.toString())
            return Plant.Type.FUNGUS.flag

        typeDao.getTaxonType(id)?.let { return it }

        var typeFlags = typeDao.getTaxonTypeByGeneric(id).fold(0) { acc, it -> acc or it }
        if (typeFlags != 0)
            return typeFlags

        typeFlags = typeDao.getTaxonTypeByFamily(id).fold(0) { acc, it -> acc or it }
        if (typeFlags != 0)
            return typeFlags

        typeFlags =  typeDao.getTaxonTypeByOrder(id).fold(0) { acc, it -> acc or it }
        if (typeFlags != 0)
            return typeFlags

        return Plant.Type.onlyPlantTypeFlags
    }

    suspend fun getTypeCount(): Int = typeDao.getTaxonCount()
}