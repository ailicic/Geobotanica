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
    fun getTaxonType(taxonId: Long): Int?

    @Query("""SELECT plantTypeFlags FROM taxonTypes JOIN (
            SELECT generic FROM taxa WHERE id=:taxonId) AS taxa
        ON taxa.generic = taxonTypes.genus""")
    fun getTaxonTypeByGeneric(taxonId: Long): List<Int>

    @Query("""SELECT plantTypeFlags FROM taxonTypes JOIN (
            SELECT family FROM taxa WHERE id=:taxonId) AS taxa
        ON taxa.family = taxonTypes.family""")
    fun getTaxonTypeByFamily(taxonId: Long): List<Int>

    @Query("""SELECT plantTypeFlags FROM taxonTypes JOIN (
            SELECT 'order' FROM taxa WHERE id=:taxonId) AS taxa
        ON 'taxa.order'= 'taxonTypes.order'""")
    fun getTaxonTypeByOrder(taxonId: Long): List<Int>

    @Query("SELECT COUNT(*) FROM taxonTypes")
    fun getTaxonCount(): Int


    // Plant Types

    @Query("""SELECT plantTypeFlags FROM vernacularTypes WHERE vernacular = (
        SELECT vernacular FROM vernaculars WHERE id = :vernacularId)""")
    fun getVernacularType(vernacularId: Long): List<Int>

    @Query("SELECT COUNT(*) FROM vernacularTypes")
    fun getVernacularCount(): Int
}