package com.geobotanica.geobotanica.ui.newplanttype

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantTypeViewModel @Inject constructor (
    val taxonRepo: TaxonRepo,
    val vernacularRepo: VernacularRepo
): ViewModel() {
    var userId = 0L
    lateinit var photoUri: String
    var commonName: String? = null
    var scientificName: String? = null
    var vernacularId: Long? = null
    var taxonId: Long? = null
    var plantTypeOptions = Plant.Type.values().toList()
    lateinit var plantType: Plant.Type
}