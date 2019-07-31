package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DECOMPRESSING

@Dao
interface OnlineAssetDao : BaseDao<OnlineAsset> {
    @Query("SELECT COUNT(*) FROM assets")
    suspend fun count(): Int

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun get(id: Long): OnlineAsset

    @Query("SELECT * FROM assets WHERE id = :id")
    fun getLiveData(id: Long): LiveData<OnlineAsset>

    @Query("SELECT * FROM assets WHERE status = :downloadId")
    suspend fun getByDownloadId(downloadId: Long): OnlineAsset?

    @Query("SELECT * FROM assets WHERE status > 0")
    suspend fun getDownloading(): List<OnlineAsset>

    @Query("SELECT * FROM assets WHERE status = :decompressing")
    suspend fun getDecompressing(decompressing: Long = DECOMPRESSING): List<OnlineAsset>

    @Query("SELECT * FROM assets")
    suspend fun getAll(): List<OnlineAsset>

    @Query("SELECT * FROM assets")
    fun getAllLiveData(): LiveData<List<OnlineAsset>>
}
