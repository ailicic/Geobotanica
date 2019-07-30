package com.geobotanica.geobotanica.ui.downloadmaps

import androidx.lifecycle.*
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.repo.GeolocationRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.Resource
import com.geobotanica.geobotanica.network.ResourceStatus.*
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SuggestedMapsViewModel @Inject constructor(
        private val storageHelper: StorageHelper,
        private val fileDownloader: FileDownloader,
        private val mapRepo: MapRepo,
        geolocationRepo: GeolocationRepo
): ViewModel() {
    var userId = 0L

    val showFab: LiveData<Boolean> = mapRepo
            .getInitiatedDownloads()
            .map { it.isNotEmpty() }

    val suggestedMaps: LiveData<Resource<List<OnlineMapListItem>>> =
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
}
