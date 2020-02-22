package com.geobotanica.geobotanica.ui.downloadassets

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.geobotanica.geobotanica.android.file.FileImporter
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.worker.AssetValidator
import com.geobotanica.geobotanica.android.worker.DownloadStatusSynchronizer
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.network.DownloadStatus.*
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.network.NetworkValidator.NetworkState.*
import com.geobotanica.geobotanica.ui.login.OnlineAssetId.*
import com.geobotanica.geobotanica.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DownloadAssetsViewModel @Inject constructor(
        private val storageHelper: StorageHelper,
        private val networkValidator: NetworkValidator,
        private val fileDownloader: FileDownloader,
        private val fileImporter: FileImporter,
        private val assetRepo: AssetRepo,
        private val assetValidator: AssetValidator,
        private val downloadStatusSynchronizer: DownloadStatusSynchronizer
): ViewModel() {
    var userId = 0L

    val showInsufficientStorageSnackbar = SingleLiveEvent<OnlineAsset>()
    val showInternetUnavailableSnackbar = SingleLiveEvent<Unit>()
    val showMeteredNetworkDialog = SingleLiveEvent<Unit>()

    val navigateToNext: LiveData<Boolean> = assetRepo.getAllLiveData().map { assetList ->
        val mapFoldersAsset = assetList.find { it.id == MAP_FOLDER_LIST.id } ?: throw IllegalStateException()
        val mapListAsset = assetList.find { it.id == MAP_LIST.id } ?: throw IllegalStateException()
        val worldMapAsset = assetList.find { it.id == WORLD_MAP.id } ?: throw IllegalStateException()
        val plantNamesAsset = assetList.find { it.id == PLANT_NAMES.id } ?: throw IllegalStateException()

        mapFoldersAsset.isDownloaded && mapListAsset.isDownloaded &&
                ! worldMapAsset.isNotDownloaded && ! plantNamesAsset.isNotDownloaded
    }

    val showDownloadButton: LiveData<Boolean> = assetRepo.getAllLiveData().map { assets ->
        val mapFoldersAsset = assets.find { it.id == MAP_FOLDER_LIST.id } ?: throw IllegalStateException()
        val mapListAsset = assets.find { it.id == MAP_LIST.id } ?: throw IllegalStateException()
        val worldMapAsset = assets.find { it.id == WORLD_MAP.id } ?: throw IllegalStateException()
        val plantNamesAsset = assets.find { it.id == PLANT_NAMES.id } ?: throw IllegalStateException()

        mapFoldersAsset.isNotDownloaded || mapListAsset.isNotDownloaded ||
                worldMapAsset.isNotDownloaded || plantNamesAsset.isNotDownloaded
    }

    val showProgressSpinner: LiveData<Boolean> = showDownloadButton.map { ! it }

    private var shouldImportAssets = false // True if assets can be imported from storage instead of downloaded

    suspend fun areAssetsInExtStorageRootDir(): Boolean = withContext(Dispatchers.IO) {
        shouldImportAssets = storageHelper.areGzipAssetsInExtStorageRootDir(assetRepo.getAll())
        shouldImportAssets
    }

    fun onMeteredNetworkAllowed() = viewModelScope.launch {
        networkValidator.allowMeteredNetwork()
        downloadAssets()
    }

    fun initAssetDownloads() {
        if (shouldImportAssets)
            downloadAssets() // Skip network check if importing
        else when (networkValidator.getStatus()) {
            INVALID -> showInternetUnavailableSnackbar.call()
            VALID_IF_METERED_PERMITTED -> showMeteredNetworkDialog.call()
            VALID -> downloadAssets()
        }
    }

    private fun downloadAssets() = viewModelScope.launch(Dispatchers.IO) {
        assetRepo.getAll().forEach { asset ->
            if (asset.isDownloading)
                Lg.d("${asset.filename}: Asset already downloading")
            else if (asset.isDownloaded)
                Lg.d("${asset.filename}: Asset already downloaded")
            else if (!storageHelper.isStorageAvailable(asset))
                showInsufficientStorageSnackbar.postValue(asset)
            else {
                val workInfo = if (shouldImportAssets)
                    fileImporter.importFromStorage(asset)
                else
                    fileDownloader.download(asset)
                assetRepo.update(asset.copy(status = DOWNLOADING))
                registerAssetObserver(workInfo, asset.id)
            }
        }
    }

    private suspend fun registerAssetObserver(workInfo: LiveData<List<WorkInfo>>, assetId: Long) = withContext(Dispatchers.Main) {
        workInfo.observeForever { // TODO: Is this the best way to catch failures? Memory leak?
            viewModelScope.launch(Dispatchers.IO) {
                val asset = assetRepo.get(assetId)
                when {
                    it.isSuccessful -> { } // NOOP
                    it.isCancelled -> {
                        Lg.d("AssetDownloadObserver: ${asset.filenameUngzip} cancelled")
                        assetValidator.verifyStatus(asset)
                    }
                    it.isFailed -> {
                        Lg.d("AssetDownloadObserver: ${asset.filenameUngzip} failed")
                        assetValidator.verifyStatus(asset)
                    }
                }
            }
        }
    }

    fun syncDownloadStatuses() = viewModelScope.launch(Dispatchers.IO) {
        downloadStatusSynchronizer.syncAll()
    }

    suspend fun getWorldMapText(): String = assetRepo.get(WORLD_MAP.id).printName

    suspend fun getPlantNameDbText(): String = assetRepo.get(PLANT_NAMES.id).printName
}

