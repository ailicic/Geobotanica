package com.geobotanica.geobotanica.data_ro.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.geobotanica.geobotanica.data_ro.entity.TaxonComposite


@Dao
interface TaxonCompositeDao {
    @Transaction @Query("SELECT * FROM taxa WHERE id = :taxonId")
    fun get(taxonId: Long): TaxonComposite

    @Transaction @Query("SELECT * FROM taxa WHERE id IN (:taxonIds)")
    fun get(taxonIds: List<Long>): List<TaxonComposite>
}