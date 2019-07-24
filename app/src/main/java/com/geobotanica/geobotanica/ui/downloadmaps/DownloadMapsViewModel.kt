package com.geobotanica.geobotanica.ui.downloadmaps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.Transformations.switchMap
import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadMapViewModel @Inject constructor(
        private val storageHelper: StorageHelper,
        private val fileDownloader: FileDownloader,
        private val mapRepo: MapRepo
//        private val geolocator: Geolocator
): ViewModel() {
    var userId = 0L

//    mapRepo.search("British Columbia").observe(this, Observer {
//        mapListAdapter.submitList(it.map { onlineMap -> onlineMap.toListItem() })
//    })

    val showFab: LiveData<Boolean> = map(mapRepo.getInitiatedDownloads()) { mapList ->
        mapList.isNotEmpty()
    }

    private var mapFolderId = MutableLiveData<Long?>().apply { value = null }
//
    val mapListItems = switchMap(mapFolderId) { mapFolderId ->
        childMapListItemsOf(mapFolderId)
    }

    fun browseMapFolder(folderId: Long?) {
        mapFolderId.value = folderId
    }

    suspend fun downloadMap(onlineMapId: Long) = withContext(Dispatchers.IO) {
        val onlineMap = mapRepo.get(onlineMapId)
        fileDownloader.downloadMap(onlineMap)
    }

    suspend fun cancelDownload(downloadId: Long) = withContext(Dispatchers.IO) {
        val result = fileDownloader.cancelDownload(downloadId)
        mapRepo.getByDownloadId(downloadId)?.let { onlineMap ->
            onlineMap.status = FileDownloader.NOT_DOWNLOADED
            mapRepo.update(onlineMap)
            Lg.i("Cancelled map download: ${onlineMap.filename} (Result=$result)")
        }
    }

    suspend fun deleteMap(onlineMapId: Long) = withContext(Dispatchers.IO) {
        val onlineMap = mapRepo.get(onlineMapId)
        val mapFile = File(storageHelper.getMapsPath(), onlineMap.filename)
        val result = mapFile.delete()
        onlineMap.status = FileDownloader.NOT_DOWNLOADED
        mapRepo.update(onlineMap)
        Lg.i("Deleted map: ${onlineMap.filename} (Result=$result)")
    }

    private fun childMapListItemsOf(mapFolderId: Long?): LiveData<List<OnlineMapListItem>> {

        val maps = mapRepo.getChildMapsOf(mapFolderId)
        val folders = mapRepo.getChildFoldersOf(mapFolderId)

        val result = MediatorLiveData<List<OnlineMapListItem>>()

        result.addSource(maps) {
            result.value = combineMapListItems(folders.value, maps.value)
        }
        result.addSource(folders) {
            result.value = combineMapListItems(folders.value, maps.value)
        }
        return result
    }

    private fun combineMapListItems(
            folders: List<OnlineMapFolder>?,
            maps: List<OnlineMap>?
    ): List<OnlineMapListItem> {

        val latestMaps = maps
        val latestFolders = folders

//        Lg.d("combineMapListItems(): latestMaps=$latestMaps; latestFolders=$latestFolders")
        if (latestMaps == null || latestFolders == null)
            return emptyList()

        return mutableListOf<OnlineMapListItem>().apply {
            addAll(latestFolders.map { it.toListItem() })
            addAll(latestMaps.map { it.toListItem() })
        }
    }
}