package com.geobotanica.geobotanica.data_taxa.dao

import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.dao.BaseDao
import com.geobotanica.geobotanica.data_taxa.entity.Tag
import org.threeten.bp.OffsetDateTime

@Dao
interface TagDao : BaseDao<Tag> {

    // Vernaculars

    @Query("SELECT * FROM tags WHERE vernacularId = :vernacularId AND tag = :tag")
    fun getVernacularWithTag(vernacularId: Long, tag: Int): Tag?

    @Query("UPDATE tags SET timestamp = :timestamp WHERE vernacularId = :vernacularId AND tag = :tag")
    fun updateVernacularTimestamp(vernacularId: Long, tag: Int, timestamp: OffsetDateTime = OffsetDateTime.now())

    @Query("DELETE FROM tags WHERE tag = :tag AND vernacularId=:id")
    fun unsetVernacularTag(id: Long, tag: Int)

    @Query("SELECT vernacularId FROM tags WHERE tag = :tag AND vernacularId NOT NULL ORDER BY timestamp DESC LIMIT :limit")
    fun getAllVernacularsWithTag(tag: Int, limit: Int): List<Long>

    @Query("""SELECT id FROM vernaculars
		WHERE id IN (SELECT vernacularId FROM tags WHERE tag = :tag AND vernacularId NOT NULL ORDER BY timestamp DESC)
		AND (vernacular LIKE :string || '%' OR vernacular LIKE "% " || :string || '%') LIMIT :limit""")
    fun taggedVernacularStartsWith(string: String, tag: Int, limit: Int): List<Long>

    @Query("""SELECT id FROM vernaculars
		WHERE id IN (SELECT vernacularId FROM tags WHERE tag = :tag AND vernacularId NOT NULL ORDER BY timestamp DESC)
		AND (vernacular LIKE :first || '%' OR vernacular LIKE "% " || :first || '%')
		AND (vernacular LIKE :second || '%' OR vernacular LIKE "% " || :second || '%') LIMIT :limit""")
    fun taggedVernacularStartsWith(first: String, second: String, tag: Int, limit: Int): List<Long>

    @Query("""SELECT id FROM vernaculars
        WHERE taxonId = :taxonId
        AND id IN (SELECT vernacularId FROM tags WHERE tag = :tag AND vernacularId NOT NULL ORDER BY timestamp DESC)""")
    fun taggedVernacularFromTaxonId(taxonId: Long, tag: Int): List<Long>


    // Taxa

    @Query("SELECT * FROM tags WHERE taxonId = :taxonId AND tag = :tag")
    fun getTaxonWithTag(taxonId: Long, tag: Int): Tag?

    @Query("UPDATE tags SET timestamp = :timestamp WHERE taxonId = :taxonId AND tag = :tag")
    fun updateTaxonTimestamp(taxonId: Long, tag: Int, timestamp: OffsetDateTime = OffsetDateTime.now())

    @Query("DELETE FROM tags WHERE taxonId=:id AND tag = :tag")
    fun unsetTaxonTag(id: Long, tag: Int)

     @Query("SELECT taxonId FROM tags WHERE tag = :tag AND taxonId NOT NULL ORDER BY timestamp DESC LIMIT :limit ")
    fun getAllTaxaWithTag(tag: Int, limit: Int): List<Long>

    @Query("""SELECT id FROM taxa
		WHERE id IN (SELECT taxonId FROM tags WHERE tag = :tag AND taxonId NOT NULL ORDER BY timestamp DESC)
		AND (generic LIKE :string || '%' OR epithet LIKE :string || '%') LIMIT :limit """)
    fun taggedTaxonStartsWith(string: String, tag: Int, limit: Int): List<Long>

    @Query("""SELECT id FROM taxa
		WHERE id IN (SELECT taxonId FROM tags WHERE tag = :tag AND taxonId NOT NULL ORDER BY timestamp DESC)
		AND (generic LIKE :first || '%' OR epithet LIKE :first || '%')
		AND (generic LIKE :second || '%' OR epithet LIKE :second || '%') LIMIT :limit """)
    fun taggedTaxonStartsWith(first: String, second: String, tag: Int, limit: Int): List<Long>

    @Query("""SELECT taxonId FROM vernaculars
        WHERE vernacular = (SELECT vernacular FROM vernaculars WHERE id=:vernacularId)
        AND taxonId IN (SELECT taxonId FROM tags WHERE tag = :tag AND taxonId NOT NULL ORDER BY timestamp DESC)""")
    fun taggedTaxonFromVernacularId(vernacularId: Long, tag: Int): List<Long>
}