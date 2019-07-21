package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DECOMPRESSING
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DOWNLOADED

@Dao
interface OnlineAssetDao : BaseDao<OnlineAsset> {
    @Query("SELECT COUNT(*) FROM assets")
    fun count(): Int

    @Query("SELECT * FROM assets WHERE id = :id")
    fun get(id: Long): OnlineAsset

    @Query("SELECT * FROM assets WHERE status = :downloadId")
    fun getByDownloadId(downloadId: Long): OnlineAsset?

    @Query("SELECT * FROM assets WHERE status > 0")
    fun getDownloading(): List<OnlineAsset>

    @Query("SELECT * FROM assets WHERE status = :decompressing")
    fun getDecompressing(decompressing: Long = DECOMPRESSING): List<OnlineAsset>

    @Query("SELECT * FROM assets")
    fun getAll(): List<OnlineAsset>

    @Query("SELECT * FROM assets")
    fun getAllLiveData(): LiveData<List<OnlineAsset>>
}
