package com.geobotanica.geobotanica.ui.newplantmeasurement

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.MeasurementRepo
import com.geobotanica.geobotanica.data.repo.PhotoRepo
import com.geobotanica.geobotanica.data.repo.PlantLocationRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.util.Lg
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantMeasurementViewModel @Inject constructor (
    val plantRepo: PlantRepo,
    val plantLocationRepo: PlantLocationRepo,
    val photoRepo: PhotoRepo,
    val measurementRepo: MeasurementRepo
): ViewModel() {

    var userId = 0L
    lateinit var plantType: Plant.Type
    lateinit var photoUri: String
    var commonName: String? = null
    var latinName: String? = null
    var height: Float? = null
    var diameter: Float? = null
    var trunkDiameter: Float? = null
    lateinit var location: Location

    fun savePlant() {
        Lg.d("Saving plant to database now...")
        val plant = Plant(userId, plantType, commonName, latinName)
        plant.id = plantRepo.insert(plant)
        Lg.d("$plant (id=${plant.id})")

        savePlantPhoto(plant)
        savePlantMeasurements(plant)
        savePlantLocation(plant)
    }

    private fun savePlantPhoto(plant: Plant) {
        val photo = Photo(userId, plant.id, Photo.Type.COMPLETE, photoUri) // TODO: Store only relative path/filename
        photo.id = photoRepo.insert(photo)
        Lg.d("$photo (id=${photo.id})")
    }

    private fun savePlantLocation(plant: Plant) {
        val plantLocation = PlantLocation(plant.id, location)
        plantLocation.id = plantLocationRepo.insert(plantLocation)
        Lg.d("$plantLocation (id=${plantLocation.id})")
    }

    private fun savePlantMeasurements(plant: Plant) {
        height?.let {
            val heightMeasurement = Measurement(userId, plant.id, Measurement.Type.HEIGHT, it)
            heightMeasurement.id = measurementRepo.insert(heightMeasurement)
            Lg.d("$heightMeasurement (id=${heightMeasurement.id})")
        }
        diameter?.let {
            val diameterMeasurement = Measurement(userId, plant.id, Measurement.Type.DIAMETER, it)
            diameterMeasurement.id = measurementRepo.insert(diameterMeasurement)
            Lg.d("$diameterMeasurement (id=${diameterMeasurement.id})")
        }
        trunkDiameter?.let {
            val trunkDiameterMeasurement = Measurement(userId, plant.id, Measurement.Type.DIAMETER, it)
            trunkDiameterMeasurement.id = measurementRepo.insert(trunkDiameterMeasurement)
            Lg.d("$trunkDiameterMeasurement (id=${trunkDiameterMeasurement.id})")
        }
    }
}