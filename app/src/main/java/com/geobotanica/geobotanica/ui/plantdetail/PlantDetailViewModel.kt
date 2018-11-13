package com.geobotanica.geobotanica.ui.plantdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.Transformations.switchMap
import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.util.Lg
import java.io.File
import javax.inject.Inject


class PlantDetailViewModel @Inject constructor(
        private val userRepo: UserRepo,
        private val plantRepo: PlantRepo,
        private val plantLocationRepo: PlantLocationRepo,
        private val plantPhotoRepo: PlantPhotoRepo,
        private val plantMeasurementRepo: PlantMeasurementRepo
): ViewModel() {
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
        user = switchMap(plant) { plant ->
            plant?.let { userRepo.get(plant.userId) }
        }

        location = map( plantLocationRepo.getLastPlantLocation(plantId) ) { it?.location }

        plantPhotos = plantPhotoRepo.getAllPhotosOfPlant(plantId)

        mainPhoto = plantPhotoRepo.getMainPhotoOfPlant(plantId)

        height = plantMeasurementRepo.getHeightOfPlant(plantId)
        heightDateText = map(height) { it?.timestamp?.toSimpleDate() ?: "" }

        diameter = plantMeasurementRepo.getDiameterOfPlant(plantId)
        diameterDateText = map(diameter) { it?.timestamp?.toSimpleDate() ?: "" }

        trunkDiameter = plantMeasurementRepo.getTrunkDiameterOfPlant(plantId)
        trunkDiameterDateText = map(trunkDiameter) { it?.timestamp?.toSimpleDate() ?: "" }

        measuredByUser = switchMap(height) {
            it?.let { height ->
                map(userRepo.get(height.userId)) { it.nickname }
            } ?: MutableLiveData<String>().apply { value = "" }
        }
        createdDateText = map(plant) {
            it?.run { timestamp.toSimpleDate() }
        }
    }

    fun deletePlant() { // TODO: Verify photos + locations are deleted (cascade policy)
        plantPhotos.value?.forEach {
            Lg.d("Deleting photo: ${it.fileName}")
            File(it.fileName).delete()
        }

        Lg.d("Deleting plant: ${plant.value!!}")
        plantRepo.delete(plant.value!!)
    }
}