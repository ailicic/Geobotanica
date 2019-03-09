package com.geobotanica.geobotanica.data_taxa.dao

import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.dao.BaseDao
import com.geobotanica.geobotanica.data_taxa.entity.StarredTaxon

@Dao
interface StarredTaxonDao : BaseDao<StarredTaxon> {

    @Query("SELECT id FROM starredTaxa")
    fun getAll(): List<Long>?

    @Query("INSERT OR IGNORE INTO starredTaxa VALUES(:id)")
    fun setStarred(id: Long)

    @Query("DELETE FROM starredTaxa WHERE id=:id")
    fun unsetStarred(id: Long)

    @Query("""SELECT id FROM taxa
		WHERE id IN (SELECT id FROM starredTaxa)
		AND (generic LIKE :string || '%' OR epithet LIKE :string || '%') LIMIT :limit """)
    fun starredStartsWith(string: String, limit: Int): List<Long>?

    @Query("""SELECT id FROM taxa
		WHERE id IN (SELECT id FROM starredTaxa)
		AND (generic LIKE :first || '%' OR epithet LIKE :first || '%')
		AND (generic LIKE :second || '%' OR epithet LIKE :second || '%') LIMIT :limit """)
    fun starredStartsWith(first: String, second: String, limit: Int): List<Long>?

    @Query("SELECT COUNT(*) FROM starredTaxa")
    fun getCount(): Int
}