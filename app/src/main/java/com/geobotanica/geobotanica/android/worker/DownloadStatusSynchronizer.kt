package com.geobotanica.geobotanica.android.worker

import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.DownloadStatus.*
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class DownloadStatusSynchronizer @Inject constructor(
        private val storageHelper: StorageHelper,
        private val assetRepo: AssetRepo,
        private val assetValidator: AssetValidator,
        private val mapRepo: MapRepo,
        private val mapValidator: MapValidator,
        private val fileDownloader: FileDownloader
) {
    suspend fun syncAll() = withContext(Dispatchers.IO) {
        syncAssets()
        syncMaps()
    }

    private suspend fun syncAssets() {
        assetRepo.getAll().forEach { asset ->
            when(asset.status) {
                NOT_DOWNLOADED -> assetValidator.verifyStatus(asset)
                DOWNLOADING -> {
                    if (! fileDownloader.isDownloading(asset)) {
                        Lg.d("syncDownloadsStatus(): Found inactive work for ${asset.filename} (updating status)")
                        fileDownloader.cancelDownloadWork(asset)
                        assetValidator.verifyStatus(asset)
                    } else
                        Lg.d("syncDownloadsStatus(): Found active work for ${asset.filename} (no changes)")
                }
                DOWNLOADED -> assetValidator.verifyStatus(asset)
            }
        }
    }

    private suspend fun syncMaps() {
        syncDownloadedMaps()
        syncDiscoveredMaps()
    }

    private suspend fun syncDownloadedMaps() {
        mapRepo.getAll().forEach { map ->
            when (map.status) {
                NOT_DOWNLOADED -> { } // NOOP
                DOWNLOADING -> {
                    if (! fileDownloader.isDownloading(map)) {
                        Lg.d("syncDownloadsStatus(): Found inactive work for ${map.filename} (updating status)")
                        fileDownloader.cancelDownloadWork(map)
                        mapValidator.verifyStatus(map)
                    } else
                        Lg.d("syncDownloadsStatus(): Found active work for ${map.filename} (no changes)")
                }
                DOWNLOADED -> mapValidator.verifyStatus(map)
            }
        }
    }

    private suspend fun syncDiscoveredMaps() {
        val mapList = mapRepo.getNotDownloaded()
        val mapFiles = File(storageHelper.getMapsPath())
                .listFiles()
                ?.filter { it.endsWith(".map") }

        mapFiles?.forEach { mapFile ->
            if (mapList.any { it.filename == mapFile.name })
                mapValidator.isValid(mapFile.name)
        }
    }
}