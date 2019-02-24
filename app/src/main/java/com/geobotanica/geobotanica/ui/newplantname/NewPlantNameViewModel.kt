package com.geobotanica.geobotanica.ui.newplantname

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data_ro.repo.TaxonRepo
import com.geobotanica.geobotanica.data_ro.repo.VernacularRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.StringBuilder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantNameViewModel @Inject constructor (
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
): ViewModel() {
    var userId = 0L
    var plantType = Plant.Type.TREE
    var photoUri: String = ""
    var commonName: String? = null
    var latinName: String? = null

    private val plantNameSearchService = PlantNameSearchService(taxonRepo, vernacularRepo)

    suspend fun searchPlantName(string: String): String? {
        return withContext(Dispatchers.IO) {
            plantNameSearchService.search(string)
        }
    }

    class PlantNameSearchService @Inject constructor (
            private val taxonRepo: TaxonRepo,
            private val vernacularRepo: VernacularRepo
    ) {
        fun search(string: String): String {
            val words = string.split(' ')
            val commonNames = vernacularRepo.startsWith(words[0])
            val commonNames2nd = vernacularRepo.secondWordStartsWith(words[0])
            val commonNamesContain = vernacularRepo.contains(words[0])
            val generalNames = taxonRepo.generalStartsWith(words[0])
            val epithetNames = taxonRepo.epithetStartsWith(words[0])

            val sb = StringBuilder()
            commonNames2nd?.forEach {
                sb.append("C2: ").append(it).append("\n")
            }
            commonNames?.forEach {
                sb.append("C: ").append(it).append("\n")
            }
            commonNamesContain?.forEach {
                sb.append("C*: ").append(it).append("\n")
            }
            generalNames?.forEach {
                sb.append("G: ").append(it).append("\n")
            }
            epithetNames?.forEach {
                sb.append("E: ").append(it).append("\n")
            }
            return sb.toString()
        }
    }
}