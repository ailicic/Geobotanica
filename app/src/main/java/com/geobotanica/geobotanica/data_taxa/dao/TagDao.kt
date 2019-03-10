package com.geobotanica.geobotanica.data_taxa.dao

import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.dao.BaseDao
import com.geobotanica.geobotanica.data_taxa.entity.Tag

@Dao
interface TagDao : BaseDao<Tag> {

    // Vernaculars

    @Query("DELETE FROM tags WHERE tag = :tag AND vernacularId=:id")
    fun unsetVernacularTag(id: Long, tag: Int)

    @Query("SELECT vernacularId FROM tags WHERE tag = :tag AND vernacularId NOT NULL LIMIT :limit")
    fun getAllVernacularsWithTag(tag: Int, limit: Int): List<Long>?

    @Query("""SELECT id FROM vernaculars
		WHERE id IN (SELECT vernacularId FROM tags WHERE tag = :tag AND vernacularId NOT NULL)
		AND (vernacular LIKE :string || '%' OR vernacular LIKE "% " || :string || '%') LIMIT :limit""")
    fun taggedVernacularStartsWith(string: String, tag: Int, limit: Int): List<Long>?

    @Query("""SELECT id FROM vernaculars
		WHERE id IN (SELECT vernacularId FROM tags WHERE tag = :tag AND vernacularId NOT NULL)
		AND (vernacular LIKE :first || '%' OR vernacular LIKE "% " || :first || '%')
		AND (vernacular LIKE :second || '%' OR vernacular LIKE "% " || :second || '%') LIMIT :limit""")
    fun taggedVernacularStartsWith(first: String, second: String, tag: Int, limit: Int): List<Long>?


    // Taxa

    @Query("DELETE FROM tags WHERE tag = :tag AND taxonId=:id ")
    fun unsetTaxonTag(id: Long, tag: Int)

    @Query("SELECT taxonId FROM tags WHERE tag = :tag AND taxonId NOT NULL LIMIT :limit ")
    fun getAllTaxaWithTag(tag: Int, limit: Int): List<Long>?

    @Query("""SELECT id FROM taxa
		WHERE id IN (SELECT taxonId FROM tags WHERE tag = :tag AND taxonId NOT NULL)
		AND (generic LIKE :string || '%' OR epithet LIKE :string || '%') LIMIT :limit """)
    fun taggedTaxonStartsWith(string: String, tag: Int, limit: Int): List<Long>?

    @Query("""SELECT id FROM taxa
		WHERE id IN (SELECT taxonId FROM tags WHERE tag = :tag AND taxonId NOT NULL)
		AND (generic LIKE :first || '%' OR epithet LIKE :first || '%')
		AND (generic LIKE :second || '%' OR epithet LIKE :second || '%') LIMIT :limit """)
    fun taggedTaxonStartsWith(first: String, second: String, tag: Int, limit: Int): List<Long>?
}