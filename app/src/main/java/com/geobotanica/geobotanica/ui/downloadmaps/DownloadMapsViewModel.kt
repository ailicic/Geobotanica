package com.geobotanica.geobotanica.ui.downloadmaps

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import javax.inject.Singleton

const val VERNACULAR_COUNT = 25021 // TODO: Get from API
const val TAXA_COUNT = 1103116 // TODO: Get from API
const val VERNACULAR_TYPE_COUNT = 32201 // TODO: Get from API
const val TAXA_TYPE_COUNT = 10340 // TODO: Get from API

@Singleton
class DownloadMapViewModel @Inject constructor(): ViewModel() {
    var userId = 0L

}