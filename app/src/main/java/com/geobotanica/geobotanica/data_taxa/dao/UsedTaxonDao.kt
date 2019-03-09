package com.geobotanica.geobotanica.data_taxa.dao

import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.dao.BaseDao
import com.geobotanica.geobotanica.data_taxa.entity.UsedTaxon

@Dao
interface UsedTaxonDao : BaseDao<UsedTaxon> {

    @Query("SELECT id FROM usedTaxa")
    fun getAll(): List<Long>?

    @Query("INSERT OR IGNORE INTO usedTaxa VALUES(:id)")
    fun setUsed(id: Long)

    @Query("DELETE FROM usedTaxa WHERE id=:id")
    fun unsetUsed(id: Long)

    @Query("""SELECT id FROM taxa
		WHERE id IN (SELECT id FROM usedTaxa)
		AND (generic LIKE :string || '%' OR epithet LIKE :string || '%') LIMIT :limit """)
    fun usedStartsWith(string: String, limit: Int): List<Long>?

    @Query("""SELECT id FROM taxa
		WHERE id IN (SELECT id FROM usedTaxa)
		AND (generic LIKE :first || '%' OR epithet LIKE :first || '%')
		AND (generic LIKE :second || '%' OR epithet LIKE :second || '%') LIMIT :limit """)
    fun usedStartsWith(first: String, second: String, limit: Int): List<Long>?

    @Query("SELECT COUNT(*) FROM usedTaxa")
    fun getCount(): Int
}