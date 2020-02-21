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
        private val fileDownloader: FileDownloader,
        private val fileImporter: FileImporter,
        private val assetRepo: AssetRepo,
        private val assetValidator: AssetValidator,
        private val downloadStatusSynchronizer: DownloadStatusSynchronizer
): ViewModel() {
    var userId = 0L
    var shouldImportAssets = false // True if assets can be imported from storage instead of downloaded

    val showStorageSnackbar = SingleLiveEvent<OnlineAsset>()

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

    suspend fun areOnlineAssetsInExtStorageRootDir(): Boolean = withContext(Dispatchers.IO) {
        shouldImportAssets = assetRepo.getAll().all { storageHelper.isGzipAssetInExtStorageRootDir(it) }
        shouldImportAssets
    }

    fun downloadAssets() = viewModelScope.launch(Dispatchers.IO) {
        if (shouldImportAssets)
            importAssets()
        else {
            assetRepo.getAll().forEach { asset ->
                if (asset.isDownloading)
                    Lg.d("${asset.filename}: Asset already downloading")
                else if (asset.isDownloaded)
                    Lg.d("${asset.filename}: Asset already downloaded")
                else if (!storageHelper.isStorageAvailable(asset))
                    showStorageSnackbar.postValue(asset)
                else {
                    val workInfo = fileDownloader.download(asset)
                    registerAssetObserver(workInfo, asset)
                    assetRepo.update(asset.copy(status = DOWNLOADING).apply { id = asset.id })
                }
            }
        }
    }

    private fun importAssets() = viewModelScope.launch(Dispatchers.IO) {
        assetRepo.getAll().forEach { asset ->
            if (asset.isDownloading)
                Lg.d("${asset.filename}: Asset already downloading")
            else if (asset.isDownloaded)
                Lg.d("${asset.filename}: Asset already downloaded")
            else if (! storageHelper.isStorageAvailable(asset))
                showStorageSnackbar.postValue(asset)
            else {
                val workInfo = fileImporter.importFromStorage(asset)
                registerAssetObserver(workInfo, asset)
                assetRepo.update(asset.copy(status = DOWNLOADING).apply { id = asset.id })
            }
        }
    }

    private suspend fun registerAssetObserver(workInfo: LiveData<List<WorkInfo>>, asset: OnlineAsset) = withContext(Dispatchers.Main) {
        workInfo.observeForever { // TODO: Is this the best way to catch failures? Memory leak?
            viewModelScope.launch(Dispatchers.IO) {
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


