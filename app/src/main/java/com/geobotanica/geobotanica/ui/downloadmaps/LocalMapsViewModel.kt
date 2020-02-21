package com.geobotanica.geobotanica.ui.downloadmaps

import androidx.lifecycle.*
import androidx.work.WorkInfo
import com.geobotanica.geobotanica.android.file.FileImporter
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.worker.DownloadStatusSynchronizer
import com.geobotanica.geobotanica.android.worker.MapValidator
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.repo.GeolocationRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.DownloadStatus.*
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.network.NetworkValidator.NetworkState.*
import com.geobotanica.geobotanica.network.Resource
import com.geobotanica.geobotanica.network.ResourceStatus.*
import com.geobotanica.geobotanica.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


// TODO: If/when tests are created, consider converting this to MVI first

@Singleton
class LocalMapsViewModel @Inject constructor(
        private val networkValidator: NetworkValidator,
        private val storageHelper: StorageHelper,
        private val fileDownloader: FileDownloader,
        private val fileImporter: FileImporter,
        private val mapRepo: MapRepo,
        private val mapValidator: MapValidator,
        private val downloadStatusSynchronizer: DownloadStatusSynchronizer,
        geolocationRepo: GeolocationRepo
): ViewModel() {
    var userId = 0L

    val localMaps: LiveData<Resource<List<OnlineMapListItem>>> =
            geolocationRepo.get().switchMap { geolocation ->
                when (geolocation.status) {
                    LOADING -> {
                        mapRepo.search(geolocation.data?.region, geolocation.data?.countryName)
                                .map { mapList ->
                                    Resource.loading(mapList.map { it.toListItem() })
                                }
                    }
                    SUCCESS -> {
                        mapRepo.search(geolocation.data?.region, geolocation.data?.countryName)
                                .map { mapList ->
                                    Resource.success(mapList.map { it.toListItem() })
                                }
                    }
                    ERROR -> liveData(Resource.error(geolocation.error ?: Throwable())) }
            }

    val showFab: LiveData<Boolean> = mapRepo
            .getInitiatedDownloadsLiveData()
            .map { it.isNotEmpty() }

    val showMeteredNetworkDialog = SingleLiveEvent<Unit>()
    val showInsufficientStorageSnackbar = SingleLiveEvent<Unit>()
    val showInternetUnavailableSnackbar = SingleLiveEvent<Unit>()

    private var lastClickedMap: OnlineMapListItem? = null

    fun getFreeExternalStorageInMb() = storageHelper.getFreeExternalStorageInMb()

    fun syncDownloadStatuses() = viewModelScope.launch(Dispatchers.IO) {
        downloadStatusSynchronizer.syncAll()
    }

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

    fun cancelDownload(mapId: Long) = viewModelScope.launch(Dispatchers.IO) {
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

    private fun downloadMap(mapId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val map = mapRepo.get(mapId)
        if (! storageHelper.isStorageAvailable(map)) {
            showInsufficientStorageSnackbar.postValue(Unit)
        } else if (storageHelper.isMapOnExtStorage(map)) {
            val workInfo = fileImporter.importFromStorage(map)
            registerMapObserver(workInfo, map)
            mapRepo.update(map.copy(status = DOWNLOADING))
        } else {
            val workInfo = fileDownloader.download(map)
            registerMapObserver(workInfo, map)
            mapRepo.update(map.copy(status = DOWNLOADING))
        }
    }


    private suspend fun registerMapObserver(workInfo: LiveData<List<WorkInfo>>, map: OnlineMap) = withContext(Dispatchers.Main) {
        workInfo.observeForever { // TODO: Is this the best way to catch failures? Memory leak?
            viewModelScope.launch(Dispatchers.IO) {
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
}
