package com.geobotanica.geobotanica.data_taxa.dao

import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.dao.BaseDao
import com.geobotanica.geobotanica.data_taxa.entity.StarredVernacular

@Dao
interface StarredVernacularDao : BaseDao<StarredVernacular> {

    @Query("SELECT id FROM starredVernaculars")
    fun getAll(): List<Long>?

    @Query("INSERT OR IGNORE INTO starredVernaculars VALUES(:id)")
    fun setStarred(id: Long)

    @Query("DELETE FROM starredVernaculars WHERE id=:id")
    fun unsetStarred(id: Long)

    @Query("""SELECT id FROM vernaculars
		WHERE id IN (SELECT id FROM starredVernaculars)
		AND (vernacular LIKE :string || '%' OR vernacular LIKE "% " || :string || '%') LIMIT :limit""")
    fun starredStartsWith(string: String, limit: Int): List<Long>?

    @Query("""SELECT id FROM vernaculars
		WHERE id IN (SELECT id FROM starredVernaculars)
		AND (vernacular LIKE :first || '%' OR vernacular LIKE "% " || :first || '%')
		AND (vernacular LIKE :second || '%' OR vernacular LIKE "% " || :second || '%') LIMIT :limit""")
    fun starredStartsWith(first: String, second: String, limit: Int): List<Long>?

    @Query("SELECT COUNT(*) FROM starredVernaculars")
    fun getCount(): Int
}