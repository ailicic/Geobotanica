package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.OnlineAssetDao
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import javax.inject.Inject


class AssetRepo @Inject constructor(private val assetDao: OnlineAssetDao) {
    suspend fun isEmpty(): Boolean = assetDao.count() == 0

    suspend fun insert(asset: OnlineAsset): Long = assetDao.insert(asset)
    suspend fun insert(assets: List<OnlineAsset>): LongArray = assetDao.insert(*(assets.toTypedArray()))

    suspend fun update(asset: OnlineAsset) = assetDao.update(asset)

    suspend fun get(id: Long): OnlineAsset = assetDao.get(id)
    suspend fun getAll(): List<OnlineAsset> = assetDao.getAll()
    fun getAllLiveData(): LiveData<List<OnlineAsset>> = assetDao.getAllLiveData()
    suspend fun getByDownloadId(downloadId: Long): OnlineAsset? = assetDao.getByDownloadId(downloadId)
    suspend fun getDownloading(): List<OnlineAsset> = assetDao.getDownloading()
    suspend fun getDecompressing(): List<OnlineAsset> = assetDao.getDecompressing()

    suspend fun delete(asset: OnlineAsset) = assetDao.delete(asset)
}