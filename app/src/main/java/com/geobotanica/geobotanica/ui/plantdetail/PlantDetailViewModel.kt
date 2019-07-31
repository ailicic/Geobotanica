package com.geobotanica.geobotanica.ui.plantdetail

import androidx.lifecycle.*
import androidx.room.withTransaction
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


class PlantDetailViewModel @Inject constructor(
        private val database: GbDatabase,
        private val userRepo: UserRepo,
        private val plantRepo: PlantRepo,
        private val plantLocationRepo: PlantLocationRepo,
        private val plantPhotoRepo: PlantPhotoRepo,
        private val plantMeasurementRepo: PlantMeasurementRepo
) : ViewModel() {
    var plantId = 0L    // Field injection of dynamic parameter.
        set(value) {
            field = value
            init()
        }

    lateinit var plant: LiveData<Plant>
    lateinit var user: LiveData<User>
    lateinit var location: LiveData<Location>

    lateinit var plantPhotos: LiveData<List<PlantPhoto>>
    lateinit var mainPhoto: LiveData<PlantPhoto>

    lateinit var height: LiveData<PlantMeasurement>
    lateinit var heightDateText: LiveData<String>

    lateinit var diameter: LiveData<PlantMeasurement>
    lateinit var diameterDateText: LiveData<String>

    lateinit var trunkDiameter: LiveData<PlantMeasurement>
    lateinit var trunkDiameterDateText: LiveData<String>

    lateinit var measuredByUser: LiveData<String>
    lateinit var locationText: LiveData<String>
    lateinit var createdDateText: LiveData<String>

    private fun init() {
        Lg.d("PlantDetailViewModel: init(plantId=$plantId)")
        plant = plantRepo.get(plantId)
        user = plant.switchMap { userRepo.get(it.userId) }

        location = plantLocationRepo.getLastPlantLocation(plantId).map { it.location }

        plantPhotos = plantPhotoRepo.getAllPhotosOfPlant(plantId)

        mainPhoto = plantPhotoRepo.getMainPhotoOfPlant(plantId)

        height = plantMeasurementRepo.getHeightOfPlant(plantId)
        heightDateText = height.map { it.timestamp.toSimpleDate() }

        diameter = plantMeasurementRepo.getDiameterOfPlant(plantId)
        diameterDateText = diameter.map { it.timestamp.toSimpleDate() }

        trunkDiameter = plantMeasurementRepo.getTrunkDiameterOfPlant(plantId)
        trunkDiameterDateText = trunkDiameter.map { it.timestamp.toSimpleDate() }

        measuredByUser = height.switchMap { height ->
            userRepo.get(height.userId).map { it.nickname }
        }
        createdDateText = plant.map { it.timestamp.toSimpleDate() }
    }

    fun deletePlant() = viewModelScope.launch {
        deletePlantPhotoFiles()
        database.withTransaction {
            Lg.d("Deleting plant: ${plant.value!!}")
            plantRepo.delete(plant.value!!)
        }
    }

    private fun deletePlantPhotoFiles() {
        plantPhotos.value?.forEach { plantPhoto ->
            val fileName = plantPhoto.fileName
            Lg.d("Deleting photo: $fileName (Result=${File(fileName).delete()})")
        }
    }
}