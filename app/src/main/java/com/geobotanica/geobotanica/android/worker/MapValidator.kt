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
    suspend fun validateAll() {
        validateDownloadedMaps()
        validateDiscoveredMaps()
    }

    suspend fun isValid(mapFilename: String): Boolean {
        val map = mapRepo.getByFilename(mapFilename) ?: throw NoSuchElementException("ValidationWorker: $mapFilename not found in map db")
        return isValid(map)
    }

    private suspend fun isValid(map: OnlineMap): Boolean {
        val mapFile = File(storageHelper.getMapsPath(), map.filename)
        return if (mapFile.exists() && mapFile.length() > map.sizeMb * 1024 * 900) { // TODO: Fix after actual map size is available. Use > 90% for now
            val changed = ! map.isDownloaded
            mapRepo.update(map.copy(status = DownloadStatus.DOWNLOADED))
//            Lg.d("${map.filename}: Validated (changed=$changed, $time ms)")
            Lg.d("${map.filename}: Validated (changed=$changed)")
            true
        } else {
            mapRepo.update(map.copy(status = DownloadStatus.NOT_DOWNLOADED))
            val result = mapFile.delete()
            Lg.e("MapValidator: ${map.filename} failed (delete file result = $result)")
            false
        }
    }

    private suspend fun validateDownloadedMaps() = mapRepo.getDownloaded().forEach { isValid(it) }

    private suspend fun validateDiscoveredMaps() {
        val mapFiles = File(storageHelper.getMapsPath()).listFiles()?.filter { it.endsWith(".map") }
        mapFiles?.forEach { isValid(it.name) }
    }
}