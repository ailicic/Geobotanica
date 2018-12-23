package com.geobotanica.geobotanica.ui.newplantmeasurement

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.PlantMeasurementRepo
import com.geobotanica.geobotanica.data.repo.PlantPhotoRepo
import com.geobotanica.geobotanica.data.repo.PlantLocationRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantMeasurementViewModel @Inject constructor (
        private val database: GbDatabase,
        private val plantRepo: PlantRepo,
        private val plantLocationRepo: PlantLocationRepo,
        private val plantPhotoRepo: PlantPhotoRepo,
        private val plantMeasurementRepo: PlantMeasurementRepo
) : ViewModel() {

    var userId = 0L
    lateinit var plantType: Plant.Type
    lateinit var photoUri: String
    var commonName: String? = null
    var latinName: String? = null
    var height: Float? = null
    var diameter: Float? = null
    var trunkDiameter: Float? = null
    lateinit var location: Location

    private var job: Job? = null

    override fun onCleared() {
        super.onCleared()
        Lg.d("NewPlantMeasurementFragment: OnCleared()")
        runBlocking { job?.join() }
    }

    fun savePlantComposite() {
        job = GlobalScope.launch(Dispatchers.IO) {
            database.runInTransaction {
                Lg.d("Saving PlantComposite to database now...")
                val plant = Plant(userId, plantType, commonName, latinName)
                plant.id = plantRepo.insert(plant)
                Lg.d("Saved: $plant (id=${plant.id})")

                savePlantPhoto(plant)
                savePlantLocation(plant)
                savePlantMeasurements(plant)
            }
        }
    }

    private fun savePlantPhoto(plant: Plant) {
        val photo = PlantPhoto(userId, plant.id, PlantPhoto.Type.COMPLETE, photoUri) // TODO: Store only relative path/filename
        photo.id = plantPhotoRepo.insert(photo)
        Lg.d("Saved: $photo (id=${photo.id})")
    }

    private fun savePlantLocation(plant: Plant) {
        val plantLocation = PlantLocation(plant.id, location)
        plantLocation.id = plantLocationRepo.insert(plantLocation)
        Lg.d("Saved: $plantLocation (id=${plantLocation.id})")
    }

    private fun savePlantMeasurements(plant: Plant) {
        height?.let {
            val heightMeasurement = PlantMeasurement(userId, plant.id, PlantMeasurement.Type.HEIGHT, it)
            heightMeasurement.id = plantMeasurementRepo.insert(heightMeasurement)
            Lg.d("Saved: $heightMeasurement (id=${heightMeasurement.id})")
        }
        diameter?.let {
            val diameterMeasurement = PlantMeasurement(userId, plant.id, PlantMeasurement.Type.DIAMETER, it)
            diameterMeasurement.id = plantMeasurementRepo.insert(diameterMeasurement)
            Lg.d("Saved: $diameterMeasurement (id=${diameterMeasurement.id})")
        }
        trunkDiameter?.let {
            val trunkDiameterMeasurement = PlantMeasurement(userId, plant.id, PlantMeasurement.Type.DIAMETER, it)
            trunkDiameterMeasurement.id = plantMeasurementRepo.insert(trunkDiameterMeasurement)
            Lg.d("Saved: $trunkDiameterMeasurement (id=${trunkDiameterMeasurement.id})")
        }
    }
}