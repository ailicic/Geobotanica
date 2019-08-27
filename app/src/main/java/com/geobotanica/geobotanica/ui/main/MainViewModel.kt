package com.geobotanica.geobotanica.ui.main

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.OnlineAssetId
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DOWNLOADED
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.NOT_DOWNLOADED
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainViewModel @Inject constructor (
        private val assetRepo: AssetRepo,
        private val mapRepo: MapRepo
): ViewModel() {

    suspend fun getStartFragmentId(): Int {
        if (assetRepo.isEmpty())
            return R.id.downloadAssetsFragment

        val onlineAssets = assetRepo.getAll()
        val mapFoldersAsset = onlineAssets.find { it.id == OnlineAssetId.MAP_FOLDER_LIST.id }!!
        val mapListAsset = onlineAssets.find { it.id == OnlineAssetId.MAP_LIST.id }!!
        val worldMapAsset = onlineAssets.find { it.id == OnlineAssetId.WORLD_MAP.id }!!
        val plantNamesAsset = onlineAssets.find { it.id == OnlineAssetId.PLANT_NAMES.id }!!


        return if (mapFoldersAsset.status != DOWNLOADED || mapListAsset.status != DOWNLOADED ||
                worldMapAsset.status == NOT_DOWNLOADED || plantNamesAsset.status == NOT_DOWNLOADED)
        {
            R.id.downloadAssetsFragment
        } else if (mapRepo.getInitiatedDownloads().isEmpty()) {
            R.id.localMapsFragment
        } else
            R.id.mapFragment
    }
}