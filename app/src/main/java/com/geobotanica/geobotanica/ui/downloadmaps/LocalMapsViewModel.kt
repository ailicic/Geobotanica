package com.geobotanica.geobotanica.ui.downloadmaps

import androidx.lifecycle.*
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.repo.GeolocationRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DOWNLOADED
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.NOT_DOWNLOADED
import com.geobotanica.geobotanica.network.Resource
import com.geobotanica.geobotanica.network.ResourceStatus.*
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class LocalMapsViewModel @Inject constructor(
        private val storageHelper: StorageHelper,
        private val fileDownloader: FileDownloader,
        private val mapRepo: MapRepo,
        geolocationRepo: GeolocationRepo
): ViewModel() {

    val showFab: LiveData<Boolean> = mapRepo
            .getInitiatedDownloadsLiveData()
            .map { it.isNotEmpty() }

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
                    ERROR -> {
                        MutableLiveData<Resource<List<OnlineMapListItem>>>()
                                .apply { value = Resource.error(geolocation.error!!) }
                    }
                }
            }

    fun getMapsFromExtStorage() = viewModelScope.launch(Dispatchers.IO) {
        File(storageHelper.getExtStorageRootDir()).listFiles().forEach { file ->
            if (file.extension == "map") {
                mapRepo.getByFilename(file.name)?.let { map ->
                    if (map.status == NOT_DOWNLOADED) {
                        Lg.d("Importing map from external storage: ${file.name}")
                        file.copyTo(File(storageHelper.getMapsPath(), file.name))
                        map.status = DOWNLOADED
                        mapRepo.update(map)
                        Lg.d("Imported map from external storage: ${file.name}")
                    }
                }
            }
        }
    }

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
}
