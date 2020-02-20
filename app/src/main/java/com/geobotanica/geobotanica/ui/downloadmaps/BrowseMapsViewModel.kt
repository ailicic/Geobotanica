package com.geobotanica.geobotanica.ui.downloadmaps

import androidx.lifecycle.*
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.DownloadStatus.NOT_DOWNLOADED
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.network.NetworkValidator.NetworkState.*
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SingleLiveEvent
import com.geobotanica.geobotanica.util.mutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// TODO: If/when tests are created, consider converting this to MVI first

@Singleton
class BrowseMapsViewModel @Inject constructor(
        private val networkValidator: NetworkValidator,
        private val storageHelper: StorageHelper,
        private val fileDownloader: FileDownloader,
        private val mapRepo: MapRepo
): ViewModel() {
    var userId = 0L

    val showFab: LiveData<Boolean> = mapRepo.getInitiatedDownloadsLiveData().map { mapList ->
        mapList.isNotEmpty()
    }

    private val mapFolderId = mutableLiveData<Long?>(null)
    val mapListItems = mapFolderId.switchMap { childMapListItemsOf(it) }

    val showMeteredNetworkDialog = SingleLiveEvent<Unit>()
    val showInsufficientStorageSnackbar = SingleLiveEvent<Unit>()
    val showInternetUnavailableSnackbar = SingleLiveEvent<Unit>()

    private var lastClickedMap: OnlineMapListItem? = null

    fun browseMapFolder(folderId: Long?) { mapFolderId.value = folderId }


    fun initDownload(mapListItem: OnlineMapListItem) {
        lastClickedMap = mapListItem
        when (networkValidator.getStatus()) {
            INVALID -> showInternetUnavailableSnackbar.call()
            VALID -> downloadMap(mapListItem.id)
            VALID_IF_METERED_PERMITTED -> showMeteredNetworkDialog.call()
        }
    }

    fun onMeteredNetworkAllowed() {
        networkValidator.allowMeteredNetwork()
        lastClickedMap?.let { downloadMap(it.id) }
    }

    fun cancelDownloadWork(onlineMapId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val onlineMap = mapRepo.get(onlineMapId)
        fileDownloader.cancelDownloadWork(onlineMap)
        Lg.i("Cancelled map download: ${onlineMap.filename}")
//        mapRepo.update(onlineMap.copy(status = NOT_DOWNLOADED))
        // TODO: Ensure status is updated in db
    }

    fun deleteMap(onlineMapId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val onlineMap = mapRepo.get(onlineMapId)
        val mapFile = File(storageHelper.getMapsPath(), onlineMap.filename)
        val result = mapFile.delete()
//        onlineMap.status = FileDownloader.NOT_DOWNLOADED
        mapRepo.update(onlineMap.copy(status = NOT_DOWNLOADED))
        Lg.i("Deleted map: ${onlineMap.filename} (Result=$result)")
    }

    private fun downloadMap(onlineMapId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val onlineMap = mapRepo.get(onlineMapId)
        if (! storageHelper.isStorageAvailable(onlineMap)) {
            showInsufficientStorageSnackbar.postValue(Unit)
            return@launch
        }
        fileDownloader.downloadMap(onlineMap)
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