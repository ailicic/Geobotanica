package com.geobotanica.geobotanica.ui.downloadassets

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadAssetsViewModel @Inject constructor(
        private val storageHelper: StorageHelper,
        private val moshi: Moshi,
        private val mapRepo: MapRepo
): ViewModel() {
    var userId = 0L

}