package com.geobotanica.geobotanica.ui.plantdetail

import androidx.lifecycle.*
import androidx.room.withTransaction
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.location.Location
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.Plant.Type.TREE
import com.geobotanica.geobotanica.data.entity.PlantMeasurement
import com.geobotanica.geobotanica.data.entity.PlantMeasurement.Type.*
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import com.geobotanica.geobotanica.data.entity.User
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.ui.viewpager.PhotoData
import com.geobotanica.geobotanica.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject


class PlantDetailViewModel @Inject constructor(
        private val dispatchers: GbDispatchers,
        private val database: GbDatabase,
        private val storageHelper: StorageHelper,
        private val userRepo: UserRepo,
        private val plantRepo: PlantRepo,
        private val plantLocationRepo: PlantLocationRepo,
        private val plantPhotoRepo: PlantPhotoRepo,
        private val plantMeasurementRepo: PlantMeasurementRepo
) : ViewModel() {

    val plant: LiveData<Plant> by lazy { plantRepo.getLiveData(plantId) }
    val plantType: LiveData<Plant.Type> by lazy { plant.map { it.type } }
    val createdByUser: LiveData<User> by lazy { plant.switchMap { userRepo.getLiveData(it.userId) } }
    val location: LiveData<Location> by lazy { plantLocationRepo.getLastPlantLocation(plantId).map { it.location } }

    private val photos: LiveData<List<PlantPhoto>> by lazy { plantPhotoRepo.getAllPhotosOfPlantLiveData(plantId) }
    val photoData: LiveData<List<PhotoData>> by lazy {
        liveData {
            val userNickname = userRepo.get(userId).nickname
            val photoData = plant.switchMap { plant -> // Note: switchMap required for auto-updating after new plant type
                photos.map { photoList ->
                    photoList.map {
                        PhotoData(
                                plant.type,
                                it.type,
                                storageHelper.photoUriFrom(it.filename),
                                userNickname,
                                it.timestamp.toDateString()
                        )
                    }
                }
            }
            emitSource(photoData)
        }
    }

    val height: LiveData<Measurement?> by lazy {
        plantMeasurementRepo.getLastHeightOfPlantLiveData(plantId).map {
            it?.let { Measurement(it.measurement).convertTo(Units.M) }
        }
    }
    val heightDateText: LiveData<String> by lazy {
        plantMeasurementRepo.getLastHeightOfPlantLiveData(plantId)
                .map { it?.timestamp?.toDateString().orEmpty() }
    }

    val diameter: LiveData<Measurement?> by lazy {
        plantMeasurementRepo.getLastDiameterOfPlantLiveData(plantId).map {
            it?.let { Measurement(it.measurement).convertTo(Units.M) }
        }
    }

    val diameterDateText: LiveData<String> by lazy {
        plantMeasurementRepo.getLastDiameterOfPlantLiveData(plantId)
                .map { it?.timestamp?.toDateString().orEmpty() }
    }

    val trunkDiameter: LiveData<Measurement?> by lazy {
        plantMeasurementRepo.getLastTrunkDiameterOfPlantLiveData(plantId).map {
            it?.let { Measurement(it.measurement) }
        }
    }
    val trunkDiameterDateText: LiveData<String> by lazy {
        plantMeasurementRepo.getLastTrunkDiameterOfPlantLiveData(plantId)
                .map { it?.timestamp?.toDateString().orEmpty() }
    }

    val lastMeasuredByUser: LiveData<String> by lazy {
        plantMeasurementRepo.getLastMeasurementOfPlant(plantId).switchMap { plantMeasurementNullable ->
            plantMeasurementNullable?.let { plantMeasurement ->
                userRepo.getLiveData(plantMeasurement.userId).map { it.nickname }
            } ?: liveData("")
        }
    }
    val createdDateText: LiveData<String> by lazy { plant.map { it.timestamp.toDateString() } }

    val startPhotoIntent = SingleLiveEvent<File>()
    val showPlantDeletedToast = SingleLiveEvent<Unit>()

    private var userId = 0L
    private var plantId = 0L
    private var isPlantMarkedForDeletion = false
    private var isPhotoRetake: Boolean = false
    private lateinit var newPhotoType: PlantPhoto.Type
    private lateinit var newPhotoUri: String

    override fun onCleared() {
        super.onCleared()
        Lg.d("PlantDetailViewModel: onCleared()")
    }

    fun init(userId: Long, plantId: Long) {
        this.userId = userId
        this.plantId = plantId
    }

    fun onDeletePhoto(currentItem: Int) {
        viewModelScope.launch(dispatchers.main) {
            deletePlantPhoto(currentItem)
            delay(300)
            showPlantDeletedToast.call()
        }
    }

    fun onRetakePhoto() {
        isPhotoRetake = true
        val photoFile = storageHelper.createPhotoFile()
        newPhotoUri = storageHelper.absolutePath(photoFile)
        startPhotoIntent.value = photoFile
    }

    fun onAddPhoto(photoType: PlantPhoto.Type) {
        isPhotoRetake = false
        newPhotoType = photoType
        val photoFile = storageHelper.createPhotoFile()
        newPhotoUri = storageHelper.absolutePath(photoFile)
        startPhotoIntent.value = photoFile
    }

    fun deleteTemporaryPhoto() {
        val result = storageHelper.deleteFile(newPhotoUri)
        Lg.d("Photo cancelled -> Deleted unused photo file: $newPhotoUri (Result = $result)")
    }

    fun onPhotoComplete(photoIndex: Int) {
        Lg.d("onPhotoComplete()")
        if (isPhotoRetake)
            updatePlantPhoto(photoIndex, newPhotoUri)
        else
            addPlantPhoto(newPhotoType, newPhotoUri)
    }

    fun onNewPlantType(newPlantType: Plant.Type) {
        viewModelScope.launch(dispatchers.io) {
            plant.value?.let { plant ->
                val updatedPlant = plant.copy(type = newPlantType).apply { id = plant.id }
                val result = plantRepo.update(updatedPlant)
                Lg.d("Updated plant type to $newPlantType (result = $result)")
            }

            // TODO: Maybe show warning before deleting all trunk diameter measurements
            val trunkDiameterList = plantMeasurementRepo.getTrunkDiametersOfPlant(plantId)
            if (newPlantType != TREE && trunkDiameterList.isNotEmpty()) {
                val result = plantMeasurementRepo.delete(*trunkDiameterList.toTypedArray())
                Lg.w("Trunk diameter measurements incompatible with new plant type: $newPlantType. Deleted $result measurements.")
            }
        }
    }

    fun onUpdatePlantNames(newCommonName: String?, newScientificName: String?) {
        viewModelScope.launch(dispatchers.io) {
            plant.value?.let { plant ->
                val updatedPlant = plant.copy(
                        commonName = newCommonName,
                        scientificName = newScientificName
                ).apply { id = plantId }
                val result = plantRepo.update(updatedPlant)
                Lg.d("Updated plant names (result = $result)")
            }
        }
    }

    fun onUpdatePhotoType(photoIndex: Int, newPhotoType: PlantPhoto.Type) {
        viewModelScope.launch(dispatchers.io) {
            photos.value?.get(photoIndex)?.let { photo ->
                val updatedPhoto = photo.copy(type = newPhotoType).apply { id = photo.id }
                val result = plantPhotoRepo.update(updatedPhoto)
                Lg.d("Updated plant photo to $newPhotoType (result = $result)")
            }
        }
    }

    fun onMeasurementsAdded(height: Measurement?, diameter: Measurement?, trunkDiameter: Measurement?) {
        viewModelScope.launch(dispatchers.io) {
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

    fun onDestroyFragment() {
        Lg.d("PlantDetailViewModel: onDestroyFragment()")
        if (isPlantMarkedForDeletion) {
            deletePlant()
            isPlantMarkedForDeletion = false
        }
    }

    fun markPlantForDeletion() { isPlantMarkedForDeletion = true }

    private fun updatePlantPhoto(photoIndex: Int, newPhotoUri: String) = viewModelScope.launch(dispatchers.io) {
        val photos = plantPhotoRepo.getAllPhotosOfPlant(plantId)
        val photo = photos[photoIndex]
        val newPhoto = photo.copy(filename = newPhotoUri.substringAfterLast('/')).apply { id = photo.id}
        val result = plantPhotoRepo.update(newPhoto)
        Lg.d("Updated plant photo (result = $result)")
    }

    private fun addPlantPhoto(photoType: PlantPhoto.Type, photoUri: String) = viewModelScope.launch(dispatchers.io) {
        plant.value?.let { plant ->
            val photo = PlantPhoto(plant.userId, plant.id, photoType, photoUri.substringAfterLast('/'))
            val id = plantPhotoRepo.insert(photo)
            Lg.d("Added plant photo (id = $id)")
        }
    }

    private suspend fun deletePlantPhoto(photoIndex: Int) = withContext(dispatchers.io) {
        photos.value?.get(photoIndex)?.let { photo ->
            val photoUri = storageHelper.photoUriFrom(photo.filename)
            Lg.d("Deleted photo file: $photoUri (result = ${File(photoUri).delete()})")
            val result = plantPhotoRepo.delete(photo)
            Lg.d("Deleted photo from db (result = $result)")
        }
    }

    private fun deletePlant() = viewModelScope.launch(dispatchers.io) {
        deleteAllPlantPhotoFiles()
        database.withTransaction {
            plant.value?.let { plant ->
                val result = plantRepo.delete(plant)
                Lg.d("Deleting plant: $plant (Result=$result)")
            }
        }
    }

    private suspend fun deleteAllPlantPhotoFiles() {
        plantPhotoRepo.getAllPhotosOfPlant(plantId).forEach { plantPhoto ->
            val photoUri = storageHelper.photoUriFrom(plantPhoto.filename)
            Lg.d("Deleting photo: $photoUri (Result=${File(photoUri).delete()})")
        }
    }
 }