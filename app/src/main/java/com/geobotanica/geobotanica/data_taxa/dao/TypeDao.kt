package com.geobotanica.geobotanica.data_taxa.dao

import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.dao.BaseDao
import com.geobotanica.geobotanica.data_taxa.entity.TaxonType

@Dao
interface TypeDao : BaseDao<TaxonType> {
    @Query("""SELECT plantTypeFlags FROM taxonTypes JOIN (
            SELECT generic, epithet FROM taxa WHERE id=:taxonId) AS taxa
        ON taxa.generic = taxonTypes.genus AND taxa.epithet = taxonTypes.epithet""")
    suspend fun getTaxonType(taxonId: Long): Int?

    @Query("""SELECT plantTypeFlags FROM taxonTypes JOIN (
            SELECT generic FROM taxa WHERE id=:taxonId) AS taxa
        ON taxa.generic = taxonTypes.genus""")
    suspend fun getTaxonTypeByGeneric(taxonId: Long): List<Int>

    @Query("""SELECT plantTypeFlags FROM taxonTypes JOIN (
            SELECT family FROM taxa WHERE id=:taxonId) AS taxa
        ON taxa.family = taxonTypes.family""")
    suspend fun getTaxonTypeByFamily(taxonId: Long): List<Int>

    @Query("""SELECT plantTypeFlags FROM taxonTypes JOIN (
            SELECT 'order' FROM taxa WHERE id=:taxonId) AS taxa
        ON 'taxa.order'= 'taxonTypes.order'""")
    suspend fun getTaxonTypeByOrder(taxonId: Long): List<Int>

    @Query("SELECT COUNT(*) FROM taxonTypes")
    suspend fun getTaxonCount(): Int


    // Plant Types

    @Query("""SELECT plantTypeFlags FROM vernacularTypes WHERE vernacular = (
        SELECT vernacular FROM vernaculars WHERE id = :vernacularId)""")
    suspend fun getVernacularType(vernacularId: Long): List<Int>

    @Query("SELECT COUNT(*) FROM vernacularTypes")
    suspend fun getVernacularCount(): Int
}