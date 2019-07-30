package com.geobotanica.geobotanica.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DECOMPRESSING
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.NOT_DOWNLOADED

@Dao
interface OnlineMapDao : BaseDao<OnlineMap> {
    @Query("SELECT * FROM maps WHERE id = :id")
    fun get(id: Long): OnlineMap

    @Query("SELECT * FROM maps")
    fun getAll(): List<OnlineMap>

    @Query("SELECT * FROM maps WHERE parentFolderId IS NULL")
    fun getAllRoot(): LiveData<List<OnlineMap>>

    @Query("SELECT * FROM maps WHERE parentFolderId = :parentFolderId")
    fun getChildMaps(parentFolderId: Long): LiveData<List<OnlineMap>>

    @Query("SELECT * FROM maps WHERE status > 0")
    fun getDownloading(): List<OnlineMap>

    @Query("SELECT * FROM maps WHERE status != :notDownloaded")
    fun getInitiatedDownloads(notDownloaded: Long = NOT_DOWNLOADED): LiveData<List<OnlineMap>>

    @Query("SELECT * FROM maps WHERE status = :decompressing")
    fun getDecompressing(decompressing: Long = DECOMPRESSING): List<OnlineMap>

    @Query("SELECT * FROM maps WHERE status = :downloadId")
    fun getByDownloadId(downloadId: Long): OnlineMap?

    @Query("""SELECT * FROM maps 
        WHERE :region IS NOT NULL AND url LIKE '%' || :region || '.map' OR
        :country IS NOT NULL AND url LIKE  '%' || :country || '.map'""")
    fun search(region: String?, country: String?): LiveData<List<OnlineMap>>
}

@Dao
interface OnlineMapFolderDao : BaseDao<OnlineMapFolder> {
    @Query("SELECT * FROM mapFolders WHERE id = :id")
    fun get(id: Long): OnlineMapFolder

    @Query("SELECT * FROM mapFolders")
    fun getAll(): List<OnlineMapFolder>

    @Query("SELECT * FROM mapFolders WHERE parentFolderId IS NULL")
    fun getAllRoot(): LiveData<List<OnlineMapFolder>>

    @Query("SELECT * FROM mapFolders WHERE parentFolderId = :parentFolderId")
    fun getChildFolders(parentFolderId: Long): LiveData<List<OnlineMapFolder>>
}
