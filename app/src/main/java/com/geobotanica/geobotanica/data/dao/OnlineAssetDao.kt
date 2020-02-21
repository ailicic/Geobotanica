package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.OnlineAsset

@Dao
interface OnlineAssetDao : BaseDao<OnlineAsset> {
    @Query("SELECT COUNT(*) FROM assets")
    suspend fun count(): Int

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun get(id: Long): OnlineAsset

    @Query("SELECT * FROM assets WHERE id = :id")
    fun getLiveData(id: Long): LiveData<OnlineAsset>

//    @Query("SELECT * FROM assets WHERE status = :downloaded")
//    suspend fun getDownloaded(downloaded: Int = DOWNLOADED.ordinal): List<OnlineAsset>

    @Query("SELECT * FROM assets")
    suspend fun getAll(): List<OnlineAsset>

    @Query("SELECT * FROM assets")
    fun getAllLiveData(): LiveData<List<OnlineAsset>>
}
