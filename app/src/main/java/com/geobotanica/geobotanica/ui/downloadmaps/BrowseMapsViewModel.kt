package com.geobotanica.geobotanica.ui.downloadmaps

import androidx.lifecycle.*
import androidx.work.WorkInfo
import com.geobotanica.geobotanica.android.file.FileImporter
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.worker.DownloadStatusSynchronizer
import com.geobotanica.geobotanica.android.worker.MapValidator
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.DownloadStatus
import com.geobotanica.geobotanica.network.DownloadStatus.NOT_DOWNLOADED
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.network.NetworkValidator.NetworkState.*
import com.geobotanica.geobotanica.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// TODO: If/when tests are created, consider converting this to MVI first

@Singleton
class BrowseMapsViewModel @Inject constructor(
        private val networkValidator: NetworkValidator,
        private val storageHelper: StorageHelper,
        private val fileDownloader: FileDownloader,
        private val fileImporter: FileImporter,
        private val mapRepo: MapRepo,
        private val mapValidator: MapValidator,
        private val downloadStatusSynchronizer: DownloadStatusSynchronizer
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

    private var lastClickedMap: OnlineMap? = null

    fun syncDownloadStatuses() = viewModelScope.launch(Dispatchers.IO) {
        downloadStatusSynchronizer.syncAll()
    }

    fun browseMapFolder(folderId: Long?) { mapFolderId.value = folderId }

    fun initDownload(mapListItem: OnlineMapListItem) = viewModelScope.launch(Dispatchers.IO) {
        val map = mapRepo.get(mapListItem.id)
        lastClickedMap = map
        if (! storageHelper.isStorageAvailable(map))
            showInsufficientStorageSnackbar.postValue(Unit)
        else if (storageHelper.isMapOnExtStorage(map))
            importMap(map)
        else when (networkValidator.getStatus()) {
            INVALID -> showInternetUnavailableSnackbar.postValue(Unit)
            VALID_IF_METERED_PERMITTED -> showMeteredNetworkDialog.postValue(Unit)
            VALID -> downloadMap(map)
        }
    }

    fun onMeteredNetworkAllowed() = viewModelScope.launch {
        networkValidator.allowMeteredNetwork()
        lastClickedMap?.let { downloadMap(it) }
    }

    private suspend fun downloadMap(map: OnlineMap) {
        val workInfo = fileDownloader.download(map)
        registerMapObserver(workInfo, map.id)
        withContext(Dispatchers.IO) {
            mapRepo.update(map.copy(status = DownloadStatus.DOWNLOADING))
        }
    }

    private suspend fun importMap(map: OnlineMap) {
        val workInfo = fileImporter.importFromStorage(map)
        registerMapObserver(workInfo, map.id)
        withContext(Dispatchers.IO) {
            mapRepo.update(map.copy(status = DownloadStatus.DOWNLOADING))
        }
    }

    private suspend fun registerMapObserver(workInfo: LiveData<List<WorkInfo>>, mapId: Long) = withContext(Dispatchers.Main) {
        workInfo.observeForever { // TODO: Is this the best way to catch failures? Memory leak?
            viewModelScope.launch(Dispatchers.IO) {
                val map = mapRepo.get(mapId)
                when {
                    it.isSuccessful -> { } // NOOP
                    it.isCancelled -> {
                        Lg.d("MapDownloadObserver: ${map.filename} cancelled")
                        mapValidator.verifyStatus(map)
                    }
                    it.isFailed -> {
                        Lg.d("MapDownloadObserver: ${map.filename} failed")
                        mapValidator.verifyStatus(map)
                    }
                }
            }
        }
    }

    fun cancelDownloadWork(mapId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val map = mapRepo.get(mapId)
        fileDownloader.cancelDownloadWork(map)
        mapRepo.update(map.copy(status = NOT_DOWNLOADED))
        Lg.i("Clicked cancel on map download: ${map.filename}")
    }

    fun deleteMap(mapId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val map = mapRepo.get(mapId)
        val mapFile = File(storageHelper.getMapsPath(), map.filename)
        val result = mapFile.delete()
        mapRepo.update(map.copy(status = NOT_DOWNLOADED))
        Lg.i("Clicked delete on map: ${map.filename} (deleted=$result)")
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