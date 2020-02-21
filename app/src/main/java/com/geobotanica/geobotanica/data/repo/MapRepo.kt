package com.geobotanica.geobotanica.data.repo

import androidx.lifecycle.LiveData
import com.geobotanica.geobotanica.data.dao.OnlineMapDao
import com.geobotanica.geobotanica.data.dao.OnlineMapFolderDao
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import com.geobotanica.geobotanica.util.formatAsMapFilename
import javax.inject.Inject


class MapRepo @Inject constructor(
        private val mapDao: OnlineMapDao,
        private val mapFolderDao: OnlineMapFolderDao
) {
    suspend fun insert(map: OnlineMap): Long = mapDao.insert(map)
    suspend fun insert(maps: List<OnlineMap>): LongArray = mapDao.insert(*(maps.toTypedArray()))
    suspend fun insertFolder(folder: OnlineMapFolder): Long = mapFolderDao.insert(folder)
    suspend fun insertFolders(folders: List<OnlineMapFolder>): LongArray = mapFolderDao.insert(*(folders.toTypedArray()))

    suspend fun update(map: OnlineMap) = mapDao.update(map)

    suspend fun get(id: Long): OnlineMap = mapDao.get(id)
    suspend fun getAll(): List<OnlineMap> = mapDao.getAll()
    suspend fun getNotDownloaded(): List<OnlineMap> = mapDao.getNotDownloaded()
    suspend fun getDownloaded(): List<OnlineMap> = mapDao.getDownloaded()
    suspend fun getInitiatedDownloads(): List<OnlineMap> = mapDao.getInitiatedDownloads()
    fun getInitiatedDownloadsLiveData(): LiveData<List<OnlineMap>> = mapDao.getInitiatedDownloadsLiveData()
    suspend fun getByFilename(filename: String): OnlineMap? = mapDao.getByFilename(filename)

    suspend fun getAllFolders(): List<OnlineMapFolder> = mapFolderDao.getAll()

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

    fun search(region: String?, country: String?): LiveData<List<OnlineMap>> =
            mapDao.search(region.formatAsMapFilename(), country.formatAsMapFilename())

    suspend fun delete(map: OnlineMap) = mapDao.delete(map)
//    suspend fun deleteFolder(folder: OnlineMapFolder) = mapFolderDao.delete(folder)
}