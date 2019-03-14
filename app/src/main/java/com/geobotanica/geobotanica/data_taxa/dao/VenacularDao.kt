package com.geobotanica.geobotanica.data_taxa.dao

import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.dao.BaseDao
import com.geobotanica.geobotanica.data_taxa.entity.Vernacular

@Dao
interface VernacularDao : BaseDao<Vernacular> {

    @Query("SELECT * FROM vernaculars WHERE id = :id")
    fun get(id: Long): Vernacular?

    @Query("""SELECT id FROM vernaculars
        WHERE vernacular LIKE "% " || :string || '%' LIMIT :limit""")
    fun nonFirstWordStartsWith(string: String, limit: Int): List<Long>?

    @Query("SELECT id FROM vernaculars WHERE vernacular LIKE :string || '%' LIMIT :limit")
    fun firstWordStartsWith(string: String, limit: Int): List<Long>?

    @Query("""SELECT id FROM vernaculars
        WHERE (vernacular LIKE :first || '%' OR vernacular LIKE "% " || :first || '%')
        AND (vernacular LIKE :second || '%' OR vernacular LIKE "% " || :second || '%') LIMIT :limit""")
    fun anyWordStartsWith(first: String, second: String, limit: Int): List<Long>?

    @Query("SELECT id FROM vernaculars WHERE taxonId = :taxonId")
    fun fromTaxonId(taxonId: Long): List<Long>?

//    @Query("SELECT taxonId FROM vernaculars WHERE vernacular LIKE '%'||:string||'%' LIMIT :limit")
//    fun contains(string: String, limit: Int = DEFAULT_RESULT_LIMIT): List<Long>?

    @Query("SELECT COUNT(*) FROM vernaculars")
    fun getCount(): Int
}