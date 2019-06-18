package com.geobotanica.geobotanica.ui.newplantconfirm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.PlantLocationRepo
import com.geobotanica.geobotanica.data.repo.PlantMeasurementRepo
import com.geobotanica.geobotanica.data.repo.PlantPhotoRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag.USED
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.Measurement
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantConfirmViewModel @Inject constructor (
        private val database: GbDatabase,
        private val plantRepo: PlantRepo,
        private val plantLocationRepo: PlantLocationRepo,
        private val plantPhotoRepo: PlantPhotoRepo,
        private val plantMeasurementRepo: PlantMeasurementRepo,
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
) : ViewModel() {

    var userId = 0L

    val plantType = MutableLiveData<Plant.Type>()
    val commonName = MutableLiveData<String>()
    val scientificName = MutableLiveData<String>()
    var taxonId: Long? = null
    var vernacularId: Long? = null
    var height = MutableLiveData<Measurement>()
    var diameter = MutableLiveData<Measurement>()
    var trunkDiameter = MutableLiveData<Measurement>()
    var location: Location? = null
    var photoUri: String = ""
//    var photoUris = mutableMapOf<PlantPhoto.Type, String>()
    var newPhotoUri: String = ""

    private var job: Job? = null

    override fun onCleared() {
        super.onCleared()
        Lg.d("NewPlantConfirmFragment: OnCleared()")
        runBlocking { job?.join() }
    }

    fun onNewPlantName(newCommonName: String, newScientificName: String) {
        nullPlantIdsIfInvalid(newCommonName, newScientificName)
        commonName.value = newCommonName
        scientificName.value = newScientificName
    }

    private fun nullPlantIdsIfInvalid(newCommonName: String, newScientificName: String) {
        vernacularId?.let {
            if (newCommonName != commonName.value)
                vernacularId = null
        }
        taxonId?.let {
            if (newScientificName != scientificName.value)
                taxonId = null
        }
    }


    fun onNewPlantMeasurements(height: Measurement?, diameter: Measurement?, trunkDiameter: Measurement?) {
        this.height.value = height
        this.diameter.value = diameter
        this.trunkDiameter.value = trunkDiameter
    }

    fun savePlantComposite() {
        job = GlobalScope.launch(Dispatchers.IO) {
            database.runInTransaction {
                Lg.d("Saving PlantComposite to database now...")
                val plant = Plant(userId, plantType.value!!, commonName.value, scientificName.value, vernacularId, taxonId)
                plant.id = plantRepo.insert(plant)
                Lg.d("Saved: $plant (id=${plant.id})")

                savePlantPhoto(plant)
                savePlantMeasurements(plant)
                savePlantLocation(plant)
            }
            vernacularId?.let {vernacularRepo.setTagged(it, USED) }
            taxonId?.let {taxonRepo.setTagged(it, USED) }
        }
    }

    private fun savePlantPhoto(plant: Plant) {
        val photo = PlantPhoto(userId, plant.id, PlantPhoto.Type.COMPLETE, photoUri) // TODO: Store only relative path/filename
        photo.id = plantPhotoRepo.insert(photo)
        Lg.d("Saved: $photo (id=${photo.id})")
    }

    private fun savePlantMeasurements(plant: Plant) {
        height.value?.let {
            val heightMeasurement = PlantMeasurement(userId, plant.id, PlantMeasurement.Type.HEIGHT, it.toCm())
            heightMeasurement.id = plantMeasurementRepo.insert(heightMeasurement)
            Lg.d("Saved: $heightMeasurement (id=${heightMeasurement.id})")
        }
        diameter.value?.let {
            val diameterMeasurement = PlantMeasurement(userId, plant.id, PlantMeasurement.Type.DIAMETER, it.toCm())
            diameterMeasurement.id = plantMeasurementRepo.insert(diameterMeasurement)
            Lg.d("Saved: $diameterMeasurement (id=${diameterMeasurement.id})")
        }
        trunkDiameter.value?.let {
            val trunkDiameterMeasurement = PlantMeasurement(userId, plant.id, PlantMeasurement.Type.TRUNK_DIAMETER, it.toCm())
            trunkDiameterMeasurement.id = plantMeasurementRepo.insert(trunkDiameterMeasurement)
            Lg.d("Saved: $trunkDiameterMeasurement (id=${trunkDiameterMeasurement.id})")
        }
    }

    private fun savePlantLocation(plant: Plant) {
        val plantLocation = PlantLocation(plant.id, location!!)
        plantLocation.id = plantLocationRepo.insert(plantLocation)
        Lg.d("Saved: $plantLocation (id=${plantLocation.id})")
    }
}