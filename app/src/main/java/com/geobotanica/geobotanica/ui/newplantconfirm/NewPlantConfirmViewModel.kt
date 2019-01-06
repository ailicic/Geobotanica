package com.geobotanica.geobotanica.ui.newplantconfirm

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.PlantLocationRepo
import com.geobotanica.geobotanica.data.repo.PlantMeasurementRepo
import com.geobotanica.geobotanica.data.repo.PlantPhotoRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
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
        private val plantMeasurementRepo: PlantMeasurementRepo
) : ViewModel() {

    var userId = 0L
    lateinit var plantType: Plant.Type
    var commonName: String? = null
    var latinName: String? = null
    var heightMeasurement: Measurement? = null
    var diameterMeasurement: Measurement? = null
    var trunkDiameterMeasurement: Measurement? = null
    var location: Location? = null
    var photoUris = mutableMapOf<PlantPhoto.Type, String>()
    var newPhotoUri: String = ""

    private var job: Job? = null

    override fun onCleared() {
        super.onCleared()
        Lg.d("NewPlantConfirmFragment: OnCleared()")
        runBlocking { job?.join() }
    }

    fun savePlantComposite() {
        job = GlobalScope.launch(Dispatchers.IO) {
            database.runInTransaction {
                Lg.d("Saving PlantComposite to database now...")
                val plant = Plant(userId, plantType, commonName, latinName)
                plant.id = plantRepo.insert(plant)
                Lg.d("Saved: $plant (id=${plant.id})")

                savePlantPhotos(plant)
                savePlantMeasurements(plant)
                savePlantLocation(plant)
            }
        }
    }

    private fun savePlantPhotos(plant: Plant) {
        photoUris.forEach { (photoType, photoUri) ->
            val photo = PlantPhoto(userId, plant.id, photoType, photoUri) // TODO: Store only relative path/filename
            photo.id = plantPhotoRepo.insert(photo)
            Lg.d("Saved: $photo (id=${photo.id})")
        }
    }

    private fun savePlantMeasurements(plant: Plant) {
        heightMeasurement?.let {
            val heightMeasurement = PlantMeasurement(userId, plant.id, PlantMeasurement.Type.HEIGHT, it.toCm())
            heightMeasurement.id = plantMeasurementRepo.insert(heightMeasurement)
            Lg.d("Saved: $heightMeasurement (id=${heightMeasurement.id})")
        }
        diameterMeasurement?.let {
            val diameterMeasurement = PlantMeasurement(userId, plant.id, PlantMeasurement.Type.DIAMETER, it.toCm())
            diameterMeasurement.id = plantMeasurementRepo.insert(diameterMeasurement)
            Lg.d("Saved: $diameterMeasurement (id=${diameterMeasurement.id})")
        }
        trunkDiameterMeasurement?.let {
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