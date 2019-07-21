package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.OnlineMapDao
import com.geobotanica.geobotanica.data.dao.OnlineMapFolderDao
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import javax.inject.Inject


class MapRepo @Inject constructor(
        private val mapDao: OnlineMapDao,
        private val mapFolderDao: OnlineMapFolderDao
) {
    fun insert(map: OnlineMap): Long = mapDao.insert(map)
    fun insert(maps: List<OnlineMap>): LongArray = mapDao.insert(*(maps.toTypedArray()))
    fun insertFolder(folder: OnlineMapFolder): Long = mapFolderDao.insert(folder)
    fun insertFolders(folders: List<OnlineMapFolder>) = folders.forEach { mapFolderDao.insert(it) }

    fun update(map: OnlineMap) = mapDao.update(map)

    fun get(id: Long): OnlineMap = mapDao.get(id)
    fun getAll(): List<OnlineMap> = mapDao.getAll()
    fun getDownloading(): List<OnlineMap> = mapDao.getDownloading()
    fun getInitiatedDownloads(): LiveData<List<OnlineMap>> = mapDao.getInitiatedDownloads()
    fun getDecompressing(): List<OnlineMap> = mapDao.getDecompressing()
    fun getByDownloadId(downloadId: Long): OnlineMap? = mapDao.getByDownloadId(downloadId)

    fun getFolder(id: Long): OnlineMapFolder = mapFolderDao.get(id)
    fun getAllFolders(): List<OnlineMapFolder> = mapFolderDao.getAll()

    fun getChildMapsOf(parentFolderId: Long?): LiveData<List<OnlineMap>> {
        return parentFolderId?.let { parentId ->
            mapDao.getChildMaps(parentId)
        } ?: mapDao.getAllRoot()
    }

    fun getChildFoldersOf(parentFolderId: Long?): LiveData<List<OnlineMapFolder>> {
        return parentFolderId?.let { parentId ->
            mapFolderDao.getChildFolders(parentId)
        } ?: mapFolderDao.getAllRoot()
    }

    fun search(string: String): LiveData<List<OnlineMap>> = mapDao.search(string.replace(' ', '-').toLowerCase())

    fun delete(map: OnlineMap) = mapDao.delete(map)
    fun deleteFolder(folder: OnlineMapFolder) = mapFolderDao.delete(folder)
}