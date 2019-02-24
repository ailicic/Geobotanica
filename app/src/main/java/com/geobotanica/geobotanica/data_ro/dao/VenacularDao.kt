package com.geobotanica.geobotanica.data_ro.dao

import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.dao.BaseDao
import com.geobotanica.geobotanica.data_ro.entity.Vernacular

@Dao
interface VernacularDao : BaseDao<Vernacular> {

    @Query("SELECT * FROM vernaculars WHERE taxonId = :taxonId")
    fun get(taxonId: Long): Vernacular?

    @Query("SELECT vernacular FROM vernaculars WHERE INSTR(vernacular, :string)=1 LIMIT 20")
    fun startsWith(string: String): List<String>?

    @Query("""SELECT vernacular FROM vernaculars
            WHERE INSTR(vernacular,' ')>0 AND INSTR(vernacular, :string)=INSTR(vernacular,' ')+1 LIMIT 20""")
    fun secondWordStartsWith(string: String): List<String>?

    @Query("SELECT vernacular FROM vernaculars WHERE vernacular LIKE :string LIMIT 20")
    fun contains(string: String): List<String>?

    @Query("SELECT COUNT(*) FROM vernaculars")
    fun getCount(): Int
}