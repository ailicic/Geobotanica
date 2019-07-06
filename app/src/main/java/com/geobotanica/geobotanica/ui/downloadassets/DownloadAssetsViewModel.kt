package com.geobotanica.geobotanica.ui.downloadassets

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadAssetsViewModel @Inject constructor(
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
): ViewModel() {
    var userId = 0L
}