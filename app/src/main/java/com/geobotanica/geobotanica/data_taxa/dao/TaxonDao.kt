package com.geobotanica.geobotanica.data_taxa.dao

import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.dao.BaseDao
import com.geobotanica.geobotanica.data_taxa.entity.Taxon

@Dao
interface TaxonDao : BaseDao<Taxon> {

    @Query("SELECT * FROM taxa WHERE id = :id")
    fun get(id: Long): Taxon?

    @Query("SELECT id FROM taxa WHERE generic LIKE :string || '%' LIMIT :limit")
    fun genericStartsWith(string: String, limit: Int): List<Long>?

    @Query("SELECT id FROM taxa WHERE epithet LIKE :string || '%' LIMIT :limit")
    fun epithetStartsWith(string: String, limit: Int): List<Long>?

    @Query("""SELECT id FROM taxa
        WHERE (generic LIKE :first || '%' OR epithet LIKE :first || '%')
        AND (generic LIKE :second || '%' OR epithet LIKE :second || '%') LIMIT :limit""")
    fun genericOrEpithetStartsWith(first: String, second: String, limit: Int): List<Long>?

    @Query("""SELECT taxonId FROM vernaculars
        WHERE vernacular = (SELECT vernacular FROM vernaculars WHERE id=:vernacularId)""")
    fun fromVernacularId(vernacularId: Long): List<Long>?

    @Query("SELECT COUNT(*) FROM taxa")
    fun getCount(): Int
}