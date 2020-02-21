package com.geobotanica.geobotanica.android.worker

import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.network.DownloadStatus.DOWNLOADED
import com.geobotanica.geobotanica.network.DownloadStatus.NOT_DOWNLOADED
import com.geobotanica.geobotanica.ui.login.OnlineAssetId.*
import com.geobotanica.geobotanica.util.Lg
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

// TODO: Get from API
private const val VERNACULAR_COUNT = 25_021
private const val TAXA_COUNT = 1_103_116
private const val VERNACULAR_TYPE_COUNT = 32_201
private const val TAXA_TYPE_COUNT = 10_340

@Singleton
class AssetValidator @Inject constructor (
        private val storageHelper: StorageHelper,
        private val assetRepo: AssetRepo,
        private val mapRepo: MapRepo,
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
) {

    suspend fun isValid(assetFilenameUngzip: String): Boolean {
        val asset = assetRepo.getAll().firstOrNull { it.filenameUngzip == assetFilenameUngzip }
                ?: throw NoSuchElementException("AssetValidator: $assetFilenameUngzip not found in asset db")
        return isValid(asset)
    }

    suspend fun verifyStatus(asset: OnlineAsset) = isValid(asset)

    private suspend fun isValid(asset: OnlineAsset): Boolean {
        val time = measureTimeMillis {
            when (asset.id) {
                MAP_FOLDER_LIST.id -> {
                    val folderCount = mapRepo.getAllFolders().count()
                    if (folderCount != asset.itemCount) {
                        onValidationFailed(asset)
                        return false
                    }
                }
                MAP_LIST.id -> {
                    val mapCount = mapRepo.getAll().count()
                    if (mapCount != asset.itemCount) {
                        onValidationFailed(asset)
                        return false
                    }
                }
                WORLD_MAP.id -> {
                    val file = File(storageHelper.getLocalPath(asset), asset.filenameUngzip)
                    if (file.length() != asset.decompressedSize) { // NOTE: Redundant (already checked by DecompressionWorker)
                        onValidationFailed(asset)
                        return false
                    }
                }
                PLANT_NAMES.id -> {
                    val taxaDbFile = File(storageHelper.getLocalPath(asset), asset.filenameUngzip)
                    if (! taxaDbFile.exists()) {
    //                    Lg.w("AssetValidator: Failed to validate ${asset.filenameUngzip} (file missing)")
                        onValidationFailed(asset)
                        return false
                    }

                    val vernacularCount = vernacularRepo.getCount()
                    val taxaCount = taxonRepo.getCount()
                    val vernacularTypeCount = vernacularRepo.getTypeCount()
                    val taxaTypeCount = taxonRepo.getTypeCount()

                    if (vernacularCount != VERNACULAR_COUNT || taxaCount != TAXA_COUNT ||
                            vernacularTypeCount != VERNACULAR_TYPE_COUNT || taxaTypeCount != TAXA_TYPE_COUNT)
                    {
    //                    Lg.w("AssetValidator: Failed to validate ${asset.filenameUngzip} " +
    //                            "(actual/exp: vernacularCount = $vernacularCount/$VERNACULAR_COUNT, " +
    //                            "taxaCount = $taxaCount/$TAXA_COUNT, " +
    //                            "vernacularTypeCount = $vernacularTypeCount/$VERNACULAR_TYPE_COUNT, " +
    //                            "taxaTypeCount = $taxaTypeCount/$TAXA_TYPE_COUNT)")
                        onValidationFailed(asset)
                        return false
                    }
                }
                else -> {
                    Lg.w("AssetValidator: Unknown asset ${asset.filename}")
                    return false
                }
            }
        }
        onValidationSuccessful(asset, time)
        return true
    }

    private suspend fun onValidationSuccessful(asset: OnlineAsset, time: Long) {
        val changed = ! asset.isDownloaded
        if (changed)
            assetRepo.update(asset.copy(status = DOWNLOADED))
        Lg.d("${asset.filenameUngzip}: Validated (changed=$changed, $time ms)")
    }

    private suspend fun onValidationFailed(asset: OnlineAsset, actualCount: Int = 0) {
        val changed = ! asset.isNotDownloaded
        if (changed)
            assetRepo.update(asset.copy(status = NOT_DOWNLOADED))
        when (asset.id) {
            MAP_FOLDER_LIST.id -> {
                Lg.w("${asset.filenameUngzip}: Failed to validate " +
                        "(changed=$changed, expected ${asset.itemCount}, found $actualCount)")
            }
            MAP_LIST.id -> {
                Lg.w("${asset.filenameUngzip}: Failed to validate " +
                        "(changed=$changed, expected ${asset.itemCount}, found $actualCount)")
            }
            WORLD_MAP.id -> {
                val result = File(storageHelper.getLocalPath(asset), asset.filenameUngzip).delete()
                Lg.w("${asset.filenameUngzip}: Failed to validate " +
                        "(changed=$changed, deleted=$result, expected ${asset.decompressedSize} b, found $actualCount b)")
            }
            PLANT_NAMES.id -> {
                val result = File(storageHelper.getLocalPath(asset), asset.filenameUngzip).delete()
                Lg.w("${asset.filenameUngzip}: Failed to validate (changed=$changed, deleted=$result)")
            }
        }
    }
}