package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import com.geobotanica.geobotanica.network.DownloadStatus.DOWNLOADED
import com.geobotanica.geobotanica.network.DownloadStatus.NOT_DOWNLOADED

@Dao
interface OnlineMapDao : BaseDao<OnlineMap> {
    @Query("SELECT * FROM maps WHERE id = :id")
    suspend fun get(id: Long): OnlineMap

    @Query("SELECT * FROM maps")
    suspend fun getAll(): List<OnlineMap>

    @Query("SELECT * FROM maps WHERE parentFolderId IS NULL")
    fun getAllRoot(): LiveData<List<OnlineMap>>

    @Query("SELECT * FROM maps WHERE parentFolderId = :parentFolderId")
    fun getChildMaps(parentFolderId: Long): LiveData<List<OnlineMap>>

    @Query("SELECT * FROM maps WHERE status = :status")
    suspend fun getNotDownloaded(status: Int = NOT_DOWNLOADED.ordinal): List<OnlineMap>

//    @Query("SELECT * FROM maps WHERE status = :status")
//    suspend fun getDownloaded(status: Int = DOWNLOADED.ordinal): List<OnlineMap>

    @Query("SELECT * FROM maps WHERE status = :status")
    fun getDownloadedLiveData(status: Int = DOWNLOADED.ordinal): LiveData<List<OnlineMap>>

    @Query("SELECT * FROM maps WHERE status != :notDownloaded")
    suspend fun getInitiatedDownloads(notDownloaded: Int = NOT_DOWNLOADED.ordinal): List<OnlineMap>

    @Query("SELECT * FROM maps WHERE status != :notDownloaded")
    fun getInitiatedDownloadsLiveData(notDownloaded: Int = NOT_DOWNLOADED.ordinal): LiveData<List<OnlineMap>>

    @Query("SELECT * FROM maps WHERE url LIKE '%' || :filename")
    suspend fun getByFilename(filename: String): OnlineMap?

    @Query("""SELECT * FROM maps 
        WHERE :region IS NOT NULL AND url LIKE '%' || :region || '.map' OR
        :country IS NOT NULL AND url LIKE  '%' || :country || '.map'""")
    fun search(region: String?, country: String?): LiveData<List<OnlineMap>>
}

@Dao
interface OnlineMapFolderDao : BaseDao<OnlineMapFolder> {
    @Query("SELECT * FROM mapFolders WHERE id = :id")
    suspend fun get(id: Long): OnlineMapFolder

    @Query("SELECT * FROM mapFolders")
    suspend fun getAll(): List<OnlineMapFolder>

    @Query("SELECT * FROM mapFolders WHERE parentFolderId IS NULL")
    fun getAllRoot(): LiveData<List<OnlineMapFolder>>

    @Query("SELECT * FROM mapFolders WHERE parentFolderId = :parentFolderId")
    fun getChildFolders(parentFolderId: Long): LiveData<List<OnlineMapFolder>>
}
