package com.geobotanica.geobotanica.data_taxa.dao

import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.dao.BaseDao
import com.geobotanica.geobotanica.data_taxa.entity.TaxonStar

@Dao
interface TaxonStarDao : BaseDao<TaxonStar> {

    @Query("SELECT id FROM taxonStars")
    fun getAll(): List<Long>?

    @Query("INSERT OR IGNORE INTO taxonStars VALUES(:id)")
    fun setStarred(id: Long)

    @Query("DELETE FROM taxonStars WHERE id=:id")
    fun unsetStarred(id: Long)

    @Query("""SELECT id FROM taxa
		WHERE id IN (SELECT id FROM taxonStars)
		AND (generic LIKE :string || '%' OR epithet LIKE :string || '%') LIMIT :limit """)
    fun starredStartsWith(string: String, limit: Int): List<Long>?

    @Query("""SELECT id FROM taxa
		WHERE id IN (SELECT id FROM taxonStars)
		AND (generic LIKE :first || '%' OR epithet LIKE :first || '%')
		AND (generic LIKE :second || '%' OR epithet LIKE :second || '%') LIMIT :limit """)
    fun starredStartsWith(first: String, second: String, limit: Int): List<Long>?

    @Query("""SELECT id FROM
            (SELECT id, generic, epithet FROM taxa WHERE generic LIKE :first || '%' OR epithet LIKE :first || '%')
        WHERE generic LIKE :second || '%' OR epithet LIKE :second || '%' LIMIT :limit""")
    fun genericOrEpithetStartsWith(first: String, second: String, limit: Int): List<Long>?

    @Query("SELECT COUNT(*) FROM taxonStars")
    fun getCount(): Int
}