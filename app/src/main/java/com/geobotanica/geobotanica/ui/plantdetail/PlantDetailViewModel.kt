package com.geobotanica.geobotanica.ui.plantdetail

import androidx.lifecycle.*
import androidx.room.withTransaction
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


class PlantDetailViewModel @Inject constructor(
        private val database: GbDatabase,
        private val userRepo: UserRepo,
        private val plantRepo: PlantRepo,
        private val plantLocationRepo: PlantLocationRepo,
        private val plantPhotoRepo: PlantPhotoRepo,
        private val plantMeasurementRepo: PlantMeasurementRepo,
        private val storageHelper: StorageHelper
) : ViewModel() {
    var isPlantMarkedForDeletion = false

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

    lateinit var height: LiveData<PlantMeasurement?>
    lateinit var heightDateText: LiveData<String>

    lateinit var diameter: LiveData<PlantMeasurement?>
    lateinit var diameterDateText: LiveData<String>

    lateinit var trunkDiameter: LiveData<PlantMeasurement?>
    lateinit var trunkDiameterDateText: LiveData<String>

    lateinit var measuredByUser: LiveData<String>
    lateinit var locationText: LiveData<String>
    lateinit var createdDateText: LiveData<String>

    fun onDestroyFragment() {
        Lg.d("PlantDetailViewModel: onDestroyFragment()")
        if (isPlantMarkedForDeletion) {
            deletePlant()
            isPlantMarkedForDeletion = false
        }
    }

    fun getPhotoUri(plantPhoto: PlantPhoto): String = storageHelper.photoUriFrom(plantPhoto.filename)

    private fun init() {
        plant = plantRepo.get(plantId)
        user = plant.switchMap { userRepo.get(it.userId) }

        location = plantLocationRepo.getLastPlantLocation(plantId).map { it.location }

        plantPhotos = plantPhotoRepo.getAllPhotosOfPlantLiveData(plantId)

        mainPhoto = plantPhotoRepo.getMainPhotoOfPlant(plantId)

        height = plantMeasurementRepo.getHeightOfPlant(plantId)
        heightDateText = height.map { it?.timestamp?.toSimpleDate().orEmpty() }

        diameter = plantMeasurementRepo.getDiameterOfPlant(plantId)
        diameterDateText = diameter.map { it?.timestamp?.toSimpleDate().orEmpty() }

        trunkDiameter = plantMeasurementRepo.getTrunkDiameterOfPlant(plantId)
        trunkDiameterDateText = trunkDiameter.map { it?.timestamp?.toSimpleDate().orEmpty() }

        measuredByUser = height.switchMap { height ->
            height?.let {
                userRepo.get(height.userId).map { it.nickname }
            } ?: MutableLiveData<String>().apply { value = "" }
        }
        createdDateText = plant.map { it.timestamp.toSimpleDate() }
    }

//    private fun deletePlant() = viewModelScope.launch(Dispatchers.IO) { // WARNING RACE CONDITION: viewModel is cleared on exit (!?) and might cancel this coroutine
    private fun deletePlant() = GlobalScope.launch(Dispatchers.IO) {
        deletePlantPhotoFiles()
        database.withTransaction {
            val result = plantRepo.delete(plant.value!!)
            Lg.d("Deleting plant: ${plant.value!!} (Result=$result)")
        }
    }

    private suspend fun deletePlantPhotoFiles() {
        plantPhotoRepo.getAllPhotosOfPlant(plantId).forEach { plantPhoto ->
            val photoUri = getPhotoUri(plantPhoto)
            Lg.d("Deleting photo: $photoUri (Result=${File(photoUri).delete()})")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Lg.d("PlantDetailViewModel: onCleared()")
    }
}