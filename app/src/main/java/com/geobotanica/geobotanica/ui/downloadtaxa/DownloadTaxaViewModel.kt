package com.geobotanica.geobotanica.ui.downloadtaxa

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

const val TAXA_DB_FILE = "taxa.db"
const val VERNACULAR_COUNT = 25021 // TODO: Get from API
const val TAXA_COUNT = 1103116 // TODO: Get from API
const val VERNACULAR_TYPE_COUNT = 32201 // TODO: Get from API
const val TAXA_TYPE_COUNT = 10340 // TODO: Get from API

const val DB_SIZE_UNGZIP = 129_412_096 // TODO: Get from API
const val DB_SIZE_GZIP = 29_038_255L // TODO: Get from API
const val url = "http://people.okanagan.bc.ca/ailicic/Markers/taxa.db.gz" // TODO: Get from API

@Singleton
class DownloadTaxaViewModel @Inject constructor(
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
): ViewModel() {
    lateinit var databasesPath: String
    var userId = 0L

    fun createDatabasesFolder() {
        val databasesDir = File(databasesPath)
        if(!databasesDir.exists()) {
            databasesDir.mkdir()
            Lg.d("Created databasesDir: $databasesDir")
        }
    }

    fun isTaxaDbDownloaded(): Boolean {
        val taxaDbFile = File(databasesPath, TAXA_DB_FILE)
        with (taxaDbFile) {
            return exists() && isFile && length() == DB_SIZE_UNGZIP.toLong()
        }
    }

    suspend fun isTaxaDbPopulated(): Boolean = withContext(Dispatchers.IO) {
        val vernacularCount = vernacularRepo.getCount()
        val taxaCount = taxonRepo.getCount()
        val vernacularTypeCount = vernacularRepo.getTypeCount()
        val taxaTypeCount = taxonRepo.getTypeCount()

        vernacularCount == VERNACULAR_COUNT && taxaCount == TAXA_COUNT &&
                vernacularTypeCount == VERNACULAR_TYPE_COUNT && taxaTypeCount == TAXA_TYPE_COUNT
    }
}