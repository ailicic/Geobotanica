package com.geobotanica.geobotanica.android.worker

import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.DownloadStatus
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DownloadStatusSynchronizer @Inject constructor(
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
                DownloadStatus.NOT_DOWNLOADED -> assetValidator.verifyStatus(asset)
                DownloadStatus.DOWNLOADING -> {
                    if (! fileDownloader.isDownloading(asset)) {
                        Lg.d("syncDownloadsStatus(): Found inactive work for ${asset.filename} (updating status)")
                        fileDownloader.cancelDownloadWork(asset)
                        assetValidator.verifyStatus(asset)
                    } else
                        Lg.d("syncDownloadsStatus(): Found active work for ${asset.filename} (no changes)")
                }
                DownloadStatus.DOWNLOADED -> assetValidator.verifyStatus(asset)
            }
        }
    }

    private suspend fun syncMaps() {
        // TODO: Update this
        mapValidator.validateAll()
    }
}