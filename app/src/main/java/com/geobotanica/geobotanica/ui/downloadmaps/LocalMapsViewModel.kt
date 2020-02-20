package com.geobotanica.geobotanica.ui.downloadmaps

import androidx.lifecycle.*
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.worker.MapValidator
import com.geobotanica.geobotanica.data.repo.GeolocationRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.DownloadStatus.DOWNLOADED
import com.geobotanica.geobotanica.network.DownloadStatus.NOT_DOWNLOADED
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.network.NetworkValidator.NetworkState.*
import com.geobotanica.geobotanica.network.Resource
import com.geobotanica.geobotanica.network.ResourceStatus.*
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SingleLiveEvent
import com.geobotanica.geobotanica.util.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// TODO: If/when tests are created, consider converting this to MVI first

@Singleton
class LocalMapsViewModel @Inject constructor(
        private val networkValidator: NetworkValidator,
        private val storageHelper: StorageHelper,
        private val fileDownloader: FileDownloader,
        private val mapRepo: MapRepo,
        private val mapValidator: MapValidator,
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

    fun getMapsFromExtStorage() = viewModelScope.launch(Dispatchers.IO) {
        File(storageHelper.getExtStorageRootPath()).listFiles()?.forEach { file ->
            if (file.extension == "map") {
                mapRepo.getByFilename(file.name)?.let { map ->
                    if (map.isNotDownloaded) {
                        Lg.d("Importing map from external storage: ${file.name}")
                        file.copyTo(File(storageHelper.getMapsPath(), file.name), overwrite = true)
                        mapRepo.update(map.copy(status = DOWNLOADED))
                        Lg.d("Imported map from external storage: ${file.name}")
                    }
                }
            }
        }
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

    fun cancelDownload(onlineMapId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val onlineMap = mapRepo.get(onlineMapId)
        fileDownloader.cancelDownloadWork(onlineMap)
        Lg.i("Cancelled map download: ${onlineMap.filename}")
        // TODO: Ensure status is updated in db
    }

//    fun cancelDownload(downloadId: Long) = viewModelScope.launch(Dispatchers.IO) {
//        val result = fileDownloader.cancelDownload(downloadId)
//        mapRepo.getByDownloadId(downloadId)?.let { onlineMap ->
//            mapRepo.update(onlineMap.copy(status = NOT_DOWNLOADED))
//            Lg.i("Cancelled map download: ${onlineMap.filename} (Result=$result)")
//        }
//    }

    fun deleteMap(onlineMapId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val onlineMap = mapRepo.get(onlineMapId)
        val mapFile = File(storageHelper.getMapsPath(), onlineMap.filename)
        val result = mapFile.delete()
        mapRepo.update(onlineMap.copy(status = NOT_DOWNLOADED))
        Lg.i("Deleted map: ${onlineMap.filename} (Result=$result)")
    }

    fun verifyMaps() = viewModelScope.launch(Dispatchers.IO) {
//        fileDownloader.verifyDownloadedMaps()
        mapValidator.validateAll()
    }

    private fun downloadMap(onlineMapId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val onlineMap = mapRepo.get(onlineMapId)
        if (! storageHelper.isStorageAvailable(onlineMap)) {
            showInsufficientStorageSnackbar.postValue(Unit)
            return@launch
        }
        fileDownloader.downloadMap(onlineMap)
    }
}
