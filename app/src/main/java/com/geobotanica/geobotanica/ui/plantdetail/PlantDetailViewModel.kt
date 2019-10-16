package com.geobotanica.geobotanica.ui.plantdetail

import androidx.lifecycle.*
import androidx.room.withTransaction
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.entity.PlantMeasurement.Type.*
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.ui.viewpager.PhotoData
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.Measurement
import com.geobotanica.geobotanica.util.Units
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

// TODO: Need to use current userId for new entries. Need to pass to detail from map via bundle.

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

    var userId = 0L

    lateinit var plant: LiveData<Plant>
    lateinit var plantType: LiveData<Plant.Type>
    lateinit var createdByUser: LiveData<User>
    lateinit var location: LiveData<Location>

    lateinit var photos: LiveData<List<PlantPhoto>>
    lateinit var photoData: LiveData<List<PhotoData>>

    lateinit var height: LiveData<Measurement?>
    lateinit var heightDateText: LiveData<String>

    lateinit var diameter: LiveData<Measurement?>
    lateinit var diameterDateText: LiveData<String>

    lateinit var trunkDiameter: LiveData<Measurement?>
    lateinit var trunkDiameterDateText: LiveData<String>

    lateinit var lastMeasuredByUser: LiveData<String>
    lateinit var createdDateText: LiveData<String>



    fun onDestroyFragment() {
        Lg.d("PlantDetailViewModel: onDestroyFragment()")
        if (isPlantMarkedForDeletion) {
            deletePlant()
            isPlantMarkedForDeletion = false
        }
    }

    private fun init() {
        plant = plantRepo.get(plantId)
        createdByUser = plant.switchMap { userRepo.get(it.userId) }

        plantType = plant.map { it.type }

        location = plantLocationRepo.getLastPlantLocation(plantId).map { it.location }

        photos = plantPhotoRepo.getAllPhotosOfPlantLiveData(plantId)
        photoData = photos.map { photoList ->
            photoList.map { PhotoData(it.type, storageHelper.photoUriFrom(it.filename), it.id) }
        }

        height = plantMeasurementRepo.getLastHeightOfPlant(plantId).map {
            it?.let { Measurement(it.measurement).convertTo(Units.M) }
        }

        heightDateText = plantMeasurementRepo.getLastHeightOfPlant(plantId)
                .map { it?.timestamp?.toSimpleDate().orEmpty() }

        diameter = plantMeasurementRepo.getLastDiameterOfPlant(plantId).map {
            it?.let { Measurement(it.measurement).convertTo(Units.M) }
        }

        diameterDateText = plantMeasurementRepo.getLastDiameterOfPlant(plantId)
                .map { it?.timestamp?.toSimpleDate().orEmpty() }

        trunkDiameter = plantMeasurementRepo.getLastTrunkDiameterOfPlant(plantId).map {
            it?.let { Measurement(it.measurement) }
        }

        trunkDiameterDateText = plantMeasurementRepo.getLastTrunkDiameterOfPlant(plantId)
                .map { it?.timestamp?.toSimpleDate().orEmpty() }

        lastMeasuredByUser = plantMeasurementRepo.getAllMeasurementsOfPlant(plantId).switchMap { measurements ->
            val lastMeasurement = measurements.maxBy { it.timestamp }
            lastMeasurement?.let { plantMeasurement ->
                userRepo.get(plantMeasurement.userId).map { it.nickname }
            } ?: MutableLiveData<String>().apply { value = "" }
        }
        createdDateText = plant.map { it.timestamp.toSimpleDate() }
    }

//    private fun deletePlant() = viewModelScope.launch(Dispatchers.IO) {
//    WARNING RACE CONDITION: viewModel is cleared on exit and might cancel this coroutine before it completes -> Use GlobalScope
    private fun deletePlant() = GlobalScope.launch(Dispatchers.IO) {
        deleteAllPlantPhotoFiles()
        database.withTransaction {
            val result = plantRepo.delete(plant.value!!)
            Lg.d("Deleting plant: ${plant.value!!} (Result=$result)")
        }
    }

    private suspend fun deleteAllPlantPhotoFiles() {
        plantPhotoRepo.getAllPhotosOfPlant(plantId).forEach { plantPhoto ->
            val photoUri = storageHelper.photoUriFrom(plantPhoto.filename)
            Lg.d("Deleting photo: $photoUri (Result=${File(photoUri).delete()})")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Lg.d("PlantDetailViewModel: onCleared()")
    }

    fun updatePlantType(plantType: Plant.Type) = viewModelScope.launch(Dispatchers.IO) {
        val plant = plant.value!!.copy(type = plantType).apply { id = plantId }
        val result = plantRepo.update(plant)
        Lg.d("Updated plant type to $plantType (result = $result)")
    }

    fun updatePlantNames(newCommonName: String?, newScientificName: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val plant = plant.value!!.copy(
                    commonName = newCommonName,
                    scientificName = newScientificName
            ).apply { id = plantId }
            val result = plantRepo.update(plant)
            Lg.d("Updated plant names (result = $result)")
        }
    }

    fun updatePlantPhotoType(photoIndex: Int, newPhotoType: PlantPhoto.Type) = viewModelScope.launch(Dispatchers.IO) {
        val photo = photos.value!![photoIndex]
        val updatedPhoto = photo.copy(type = newPhotoType).apply { id = photo.id }
        val result = plantPhotoRepo.update(updatedPhoto)
        Lg.d("Updated plant photo to $newPhotoType (result = $result)")
    }

    fun updatePlantPhoto(photoIndex: Int, newPhotoUri: String) = viewModelScope.launch(Dispatchers.IO) {
        val photos = plantPhotoRepo.getAllPhotosOfPlant(plantId)
        val photo = photos[photoIndex]
        val newPhoto = photo.copy(filename = newPhotoUri.substringAfterLast('/')).apply { id = photo.id}
        val result = plantPhotoRepo.update(newPhoto)
        Lg.d("Updated plant photo (result = $result)")
    }

    fun addPlantPhoto(photoType: PlantPhoto.Type, photoUri: String) = viewModelScope.launch(Dispatchers.IO) {
        val plant = plant.value!!
        val photo = PlantPhoto(plant.userId, plant.id, photoType, photoUri.substringAfterLast('/'))
        val id = plantPhotoRepo.insert(photo)
        Lg.d("Added plant photo (id = $id)")
    }

    suspend fun deletePlantPhoto(photoIndex: Int) = withContext(Dispatchers.IO) {
        val photo = photos.value!![photoIndex]
        val photoUri = storageHelper.photoUriFrom(photo.filename)
        Lg.d("Deleted photo file: $photoUri (result = ${File(photoUri).delete()})")
        val result = plantPhotoRepo.delete(photo)
        Lg.d("Deleted photo from db (result = $result)")
    }

    fun onMeasurementsAdded(height: Measurement?, diameter: Measurement?, trunkDiameter: Measurement?) {
        viewModelScope.launch(Dispatchers.IO) {
            database.withTransaction {
                height?.let {
                    val heightMeasurement = PlantMeasurement(userId, plantId, HEIGHT, it.toCm())
                    heightMeasurement.id = plantMeasurementRepo.insert(heightMeasurement)
                    Lg.d("Saved: $heightMeasurement (id=${heightMeasurement.id})")
                }
                diameter?.let {
                    val diameterMeasurement = PlantMeasurement(userId, plantId, DIAMETER, it.toCm())
                    diameterMeasurement.id = plantMeasurementRepo.insert(diameterMeasurement)
                    Lg.d("Saved: $diameterMeasurement (id=${diameterMeasurement.id})")
                }
                trunkDiameter?.let {
                    val trunkDiameterMeasurement = PlantMeasurement(userId, plantId, TRUNK_DIAMETER, it.toCm())
                    trunkDiameterMeasurement.id = plantMeasurementRepo.insert(trunkDiameterMeasurement)
                    Lg.d("Saved: $trunkDiameterMeasurement (id=${trunkDiameterMeasurement.id})")
                }
            }
        }
    }
 }