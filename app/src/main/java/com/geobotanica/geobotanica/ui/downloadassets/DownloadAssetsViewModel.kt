package com.geobotanica.geobotanica.ui.downloadassets

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.data.entity.OnlineAssetId
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DECOMPRESSING
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DOWNLOADED
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.NOT_DOWNLOADED
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadAssetsViewModel @Inject constructor(
        private val storageHelper: StorageHelper,
        private val fileDownloader: FileDownloader,
        private val assetRepo: AssetRepo
): ViewModel() {
    var userId = 0L

    val showStorageSnackbar = SingleLiveEvent<OnlineAsset>()

    val navigateToNext: LiveData<Boolean> = assetRepo.getAllLiveData().map { assets ->
        val mapFoldersAsset = assets.find { it.id == OnlineAssetId.MAP_FOLDER_LIST.id } ?: throw IllegalStateException()
        val mapListAsset = assets.find { it.id == OnlineAssetId.MAP_LIST.id } ?: throw IllegalStateException()
        val worldMapAsset = assets.find { it.id == OnlineAssetId.WORLD_MAP.id } ?: throw IllegalStateException()
        val plantNamesAsset = assets.find { it.id == OnlineAssetId.PLANT_NAMES.id } ?: throw IllegalStateException()

        mapFoldersAsset.status == DOWNLOADED && mapListAsset.status == DOWNLOADED &&
                worldMapAsset.status != NOT_DOWNLOADED && plantNamesAsset.status != NOT_DOWNLOADED
    }

    init {
        viewModelScope.launch (Dispatchers.IO) {
//             fileDownloader.removeQueuedDownloads()
            importOnlineAssetInfo()
        }
    }

    fun downloadAssets() = viewModelScope.launch(Dispatchers.IO) {
        assetRepo.getAll().forEach { asset ->
            if (asset.isDownloading) {
                Lg.d("Asset already downloading: ${asset.filenameGzip}")
                return@forEach
            } else if (asset.status == DECOMPRESSING) {
                Lg.d("Asset already decompressing: ${asset.filenameGzip}")
                return@forEach
            } else if (asset.status == DOWNLOADED) {
                Lg.d("Asset already available: ${asset.filename}")
                return@forEach
            } else if (!storageHelper.isStorageAvailable(asset)) {
                showStorageSnackbar.postValue(asset)
                return@forEach
            } else
                fileDownloader.downloadAsset(asset)
        }
    }

    private suspend fun importOnlineAssetInfo() {
        if (assetRepo.isEmpty())
            assetRepo.insert(onlineAssetList)
    }

    suspend fun getWorldMapText(): String = assetRepo.get(OnlineAssetId.WORLD_MAP.id).printName

    suspend fun getPlantNameDbText(): String = assetRepo.get(OnlineAssetId.PLANT_NAMES.id).printName
}


// TODO: Get from API
val onlineAssetList = listOf(
    OnlineAsset(
        "Map metadata",
        "http://people.okanagan.bc.ca/ailicic/Maps/map_folders.json.gz",
        "",
        false,
        353,
        1_407
    ),
    OnlineAsset(
        "Map list",
        "http://people.okanagan.bc.ca/ailicic/Maps/maps.json.gz",
        "",
        false,
        5_792,
        42_619
    ),
    OnlineAsset(
        "World map",
        "http://people.okanagan.bc.ca/ailicic/Maps/world.map.gz",
        "maps",
        false,
        2_715_512,
        3_276_950
    ),
    OnlineAsset(
        "Plant name database",
        "http://people.okanagan.bc.ca/ailicic/Maps/taxa.db.gz",
        "databases",
        true,
        29_038_255,
        129_412_096
    )
)