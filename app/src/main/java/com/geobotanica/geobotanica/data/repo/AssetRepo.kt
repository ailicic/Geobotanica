package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.OnlineAssetDao
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import javax.inject.Inject


class AssetRepo @Inject constructor(private val assetDao: OnlineAssetDao) {
    fun isEmpty(): Boolean = assetDao.count() == 0

    fun insert(asset: OnlineAsset): Long = assetDao.insert(asset)
    fun insert(assets: List<OnlineAsset>): LongArray = assetDao.insert(*(assets.toTypedArray()))

    fun update(asset: OnlineAsset) = assetDao.update(asset)

    fun get(id: Long): OnlineAsset = assetDao.get(id)
    fun getAll(): List<OnlineAsset> = assetDao.getAll()
    fun getAllLiveData(): LiveData<List<OnlineAsset>> = assetDao.getAllLiveData()
    fun getByDownloadId(downloadId: Long): OnlineAsset? = assetDao.getByDownloadId(downloadId)
    fun getDownloading(): List<OnlineAsset> = assetDao.getDownloading()
    fun getDecompressing(): List<OnlineAsset> = assetDao.getDecompressing()

    fun delete(asset: OnlineAsset) = assetDao.delete(asset)
}