package com.geobotanica.geobotanica.data_taxa

import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val VERNACULAR_COUNT = 25021 // TODO: Get from API
private const val TAXA_COUNT = 1103116 // TODO: Get from API
private const val VERNACULAR_TYPE_COUNT = 32201 // TODO: Get from API
private const val TAXA_TYPE_COUNT = 10340 // TODO: Get from API

@Singleton
class TaxaDatabaseValidator @Inject constructor(
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
) {
    suspend fun isPopulated(): Boolean = withContext(Dispatchers.IO) {
        val vernacularCount = vernacularRepo.getCount()
        val taxaCount = taxonRepo.getCount()
        val vernacularTypeCount = vernacularRepo.getTypeCount()
        val taxaTypeCount = taxonRepo.getTypeCount()

        vernacularCount == VERNACULAR_COUNT && taxaCount == TAXA_COUNT &&
                vernacularTypeCount == VERNACULAR_TYPE_COUNT && taxaTypeCount == TAXA_TYPE_COUNT
    }
}