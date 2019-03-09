package com.geobotanica.geobotanica.data_taxa.dao

import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.dao.BaseDao
import com.geobotanica.geobotanica.data_taxa.entity.UsedVernacular

@Dao
interface UsedVernacularDao : BaseDao<UsedVernacular> {

    @Query("SELECT id FROM usedVernaculars")
    fun getAll(): List<Long>?

    @Query("INSERT OR IGNORE INTO usedVernaculars VALUES(:id)")
    fun setUsed(id: Long)

    @Query("DELETE FROM usedVernaculars WHERE id=:id")
    fun unsetUsed(id: Long)

    @Query("""SELECT id FROM vernaculars
		WHERE id IN (SELECT id FROM usedVernaculars)
		AND (vernacular LIKE :string || '%' OR vernacular LIKE "% " || :string || '%') LIMIT :limit""")
    fun usedStartsWith(string: String, limit: Int): List<Long>?

    @Query("""SELECT id FROM vernaculars
		WHERE id IN (SELECT id FROM usedVernaculars)
		AND (vernacular LIKE :first || '%' OR vernacular LIKE "% " || :first || '%')
		AND (vernacular LIKE :second || '%' OR vernacular LIKE "% " || :second || '%') LIMIT :limit""")
    fun usedStartsWith(first: String, second: String, limit: Int): List<Long>?

    @Query("SELECT COUNT(*) FROM usedVernaculars")
    fun getCount(): Int
}