package com.geobotanica.geobotanica.android.worker

import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.DownloadStatus
import com.geobotanica.geobotanica.util.Lg
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class MapValidator @Inject constructor(
        private val storageHelper: StorageHelper,
        private val mapRepo: MapRepo
) {

    suspend fun isValid(mapFilename: String): Boolean {
        val map = mapRepo.getByFilename(mapFilename) ?: throw NoSuchElementException("ValidationWorker: $mapFilename not found in map db")
        return isValid(map)
    }

    suspend fun verifyStatus(map: OnlineMap) = isValid(map)

    private suspend fun isValid(map: OnlineMap): Boolean {
        val mapFile = File(storageHelper.getMapsPath(), map.filename)
        val mapFileLength = mapFile.length()
        return if (mapFile.exists()
                && mapFileLength > map.sizeMb * 1024 * 900 // TODO: Fix after actual map size is available. Use 10% tolerance for now.
                && mapFileLength < map.sizeMb * 1024 * 1100
        ) {
            val changed = ! map.isDownloaded
            Lg.d("${map.filename}: Validated (changed=$changed)")
            mapRepo.update(map.copy(status = DownloadStatus.DOWNLOADED).apply { id = map.id })
            true
        } else {
            val changed = ! map.isNotDownloaded
            val result = mapFile.delete()
            Lg.w("MapValidator: ${map.filename} failed (changed=$changed, deleted=$result)")
            mapRepo.update(map.copy(status = DownloadStatus.NOT_DOWNLOADED).apply { id = map.id })
            false
        }
    }
}