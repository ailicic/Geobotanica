package com.geobotanica.geobotanica.data_ro.dao

import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.dao.BaseDao
import com.geobotanica.geobotanica.data_ro.entity.Taxon

@Dao
interface TaxonDao : BaseDao<Taxon> {

    @Query("SELECT * FROM taxa WHERE taxonId = :taxonId")
    fun get(taxonId: Long): Taxon?

    @Query("SELECT generic || ' ' || epithet FROM taxa WHERE INSTR(generic, :string)=1 LIMIT :limit")
    fun generalStartsWith(string: String, limit: Int = 20): List<String>?

    @Query("SELECT generic || ' ' || epithet FROM taxa WHERE INSTR(epithet, :string)=1 LIMIT :limit")
    fun epithetStartsWith(string: String, limit: Int = 20): List<String>?

    @Query("SELECT COUNT(*) FROM taxa")
    fun getCount(): Int
}