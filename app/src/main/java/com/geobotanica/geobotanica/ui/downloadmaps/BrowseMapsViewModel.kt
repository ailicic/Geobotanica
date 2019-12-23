package com.geobotanica.geobotanica.ui.downloadmaps

import androidx.lifecycle.*
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.mutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowseMapsViewModel @Inject constructor(
        private val storageHelper: StorageHelper,
        private val fileDownloader: FileDownloader,
        private val mapRepo: MapRepo
): ViewModel() {
    val showFab: LiveData<Boolean> = mapRepo.getInitiatedDownloadsLiveData().map { mapList ->
        mapList.isNotEmpty()
    }

    private val mapFolderId = mutableLiveData<Long?>(null)

    val mapListItems = mapFolderId.switchMap { childMapListItemsOf(it) }

    fun browseMapFolder(folderId: Long?) { mapFolderId.value = folderId }

    fun downloadMap(onlineMapId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val onlineMap = mapRepo.get(onlineMapId)
        fileDownloader.downloadMap(onlineMap)
    }

    fun cancelDownload(downloadId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val result = fileDownloader.cancelDownload(downloadId)
        mapRepo.getByDownloadId(downloadId)?.let { onlineMap ->
            onlineMap.status = FileDownloader.NOT_DOWNLOADED
            mapRepo.update(onlineMap)
            Lg.i("Cancelled map download: ${onlineMap.filename} (Result=$result)")
        }
    }

    fun deleteMap(onlineMapId: Long) = viewModelScope.launch(Dispatchers.IO) {
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

//        Lg.d("combineMapListItems(): latestMaps=$latestMaps; latestFolders=$latestFolders")
        if (maps == null || folders == null)
            return emptyList()

        return mutableListOf<OnlineMapListItem>().apply {
            addAll(folders.map { it.toListItem() })
            addAll(maps.map { it.toListItem() })
        }
    }
}