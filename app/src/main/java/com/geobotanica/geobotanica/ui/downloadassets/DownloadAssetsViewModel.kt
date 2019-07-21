package com.geobotanica.geobotanica.ui.downloadassets

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.data.entity.OnlineAssetId
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DECOMPRESSING
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DOWNLOADED
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.NOT_DOWNLOADED
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SingleLiveEvent
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadAssetsViewModel @Inject constructor(
        private val storageHelper: StorageHelper,
        private val moshi: Moshi,
        private val fileDownloader: FileDownloader,
        private val assetRepo: AssetRepo,
        private val mapRepo: MapRepo
): ViewModel() {
    var userId = 0L

    val showStorageSnackbar = SingleLiveEvent<OnlineAsset>()

    val navigateToNext: LiveData<Boolean> = map(assetRepo.getAllLiveData()) { assets ->
        val mapFoldersAsset = assets.find { it.id == OnlineAssetId.MAP_FOLDER_LIST.id }!!
        val mapListAsset = assets.find { it.id == OnlineAssetId.MAP_LIST.id }!!
        val worldMapAsset = assets.find { it.id == OnlineAssetId.WORLD_MAP.id }!!
        val plantNamesAsset = assets.find { it.id == OnlineAssetId.PLANT_NAMES.id }!!

        mapFoldersAsset.status == DOWNLOADED && mapListAsset.status == DOWNLOADED &&
                worldMapAsset.status != NOT_DOWNLOADED && plantNamesAsset.status != NOT_DOWNLOADED
    }

    init {
        importOnlineAssetInfo()
    }

    suspend fun downloadAssets() = withContext(Dispatchers.IO) {
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

    private fun importOnlineAssetInfo() = viewModelScope.launch (Dispatchers.IO) {
        if (assetRepo.isEmpty())
            assetRepo.insert(onlineAssetList)
    }

    // TODO: Get from API
    private val onlineAssetList = listOf(
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
            "http://people.okanagan.bc.ca/ailicic/Markers/taxa.db.gz",
            "databases",
            true,
            29_038_255,
            129_412_096
        )
    )
}