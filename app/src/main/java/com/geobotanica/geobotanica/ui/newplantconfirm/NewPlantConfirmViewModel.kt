package com.geobotanica.geobotanica.ui.newplantconfirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.location.Location
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.Plant.Type.TREE
import com.geobotanica.geobotanica.data.entity.PlantLocation
import com.geobotanica.geobotanica.data.entity.PlantMeasurement
import com.geobotanica.geobotanica.data.entity.PlantMeasurement.Type.*
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag.USED
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.ui.viewpager.PhotoData
import com.geobotanica.geobotanica.util.GbDispatchers
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.Measurement
import com.geobotanica.geobotanica.util.SingleLiveEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantConfirmViewModel @Inject constructor (
        private val dispatchers: GbDispatchers,
        private val database: GbDatabase,
        private val storageHelper: StorageHelper,
        private val userRepo: UserRepo,
        private val plantRepo: PlantRepo,
        private val plantLocationRepo: PlantLocationRepo,
        private val plantPhotoRepo: PlantPhotoRepo,
        private val plantMeasurementRepo: PlantMeasurementRepo,
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
) : ViewModel() {

    private val _commonName = MutableLiveData<String>()
    val commonName: LiveData<String> = _commonName // Used for xml data-binding

    private val _scientificName = MutableLiveData<String>()
    val scientificName: LiveData<String> = _scientificName // Used for xml data-binding

    private lateinit var _plantType: Plant.Type
    val plantType: Plant.Type get() = _plantType

    private val _height = MutableLiveData<Measurement>()
    val height: LiveData<Measurement> = _height // Used for xml data-binding

    private val _diameter = MutableLiveData<Measurement>()
    val diameter: LiveData<Measurement> = _diameter // Used for xml data-binding

    private val _trunkDiameter = MutableLiveData<Measurement>()
    val trunkDiameter: LiveData<Measurement> = _trunkDiameter // Used for xml data-binding

    private val _photoData = MutableLiveData<List<PhotoData>>()
    val photoData: LiveData<List<PhotoData>> = _photoData

    val startPhotoIntent = SingleLiveEvent<File>()
    val showPhotoDeletedToast = SingleLiveEvent<Unit>()

    private var userId = 0L
    private lateinit var userNickname: String
    private var taxonId: Long? = null
    private var vernacularId: Long? = null
    private var isPhotoRetake: Boolean = false
    private lateinit var newPhotoType: PlantPhoto.Type
    private lateinit var newPhotoUri: String

    fun init(
            userId: Long,
            photoUri: String,
            commonName: String?,
            scientificName: String?,
            vernacularId: Long?,
            taxonId: Long?,
            plantType: Plant.Type,
            height: Measurement?,
            diameter: Measurement?,
            trunkDiameter: Measurement?
    ) {
        this.userId = userId
        this.vernacularId = vernacularId
        this.taxonId = taxonId

        _commonName.value = commonName
        _scientificName.value = scientificName
        _plantType = plantType
        _height.value = height
        _diameter.value = diameter
        _trunkDiameter.value = trunkDiameter

        viewModelScope.launch(dispatchers.main) {
            userNickname = userRepo.get(userId).nickname
            _photoData.value = mutableListOf(PhotoData(plantType, PlantPhoto.Type.COMPLETE, photoUri, userNickname))
        }

        Lg.d("Fragment args: userId=$userId, photoType=$plantType, " +
                "commonName=$commonName, scientificName=$scientificName, " +
                "vernId=$vernacularId, taxonId=$taxonId, photos=$photoData, " +
                "height=$height, diameter=$diameter, trunkDiameter=$trunkDiameter")
    }



    fun deletePhoto(photoIndex: Int) {
        photoData.value?.let { photoData ->
            val photoUri = photoData[photoIndex].photoUri
            val result = storageHelper.deleteFile(photoUri)
            Lg.d("Deleting old photo: $photoUri (Result = $result)")
            viewModelScope.launch(dispatchers.main) {
                delay(300)
                _photoData.value = photoData.toMutableList().apply { removeAt(photoIndex) }
                showPhotoDeletedToast.call()
            }
        }
    }

    fun retakePhoto() {
        isPhotoRetake = true
        val photoFile = storageHelper.createPhotoFile()
        newPhotoUri = storageHelper.getAbsolutePath(photoFile)
        startPhotoIntent.value = photoFile
    }

    fun addPhoto(photoType: PlantPhoto.Type) {
        isPhotoRetake = false
        newPhotoType = photoType
        val photoFile = storageHelper.createPhotoFile()
        newPhotoUri = storageHelper.getAbsolutePath(photoFile)
        startPhotoIntent.value = photoFile
    }

    fun deleteTemporaryPhoto() {
        val result = storageHelper.deleteFile(newPhotoUri)
        Lg.d("Photo cancelled -> Deleted unused photo file: $newPhotoUri (Result = $result)")
    }

    fun updatePhotoType(photoIndex: Int, photoType: PlantPhoto.Type) {
        photoData.value?.let { photoData ->
            val newPhoto = photoData[photoIndex].copy(photoType = photoType)
            _photoData.value = photoData.toMutableList().apply { set(photoIndex, newPhoto) }
        }
    }

    fun deleteAllPhotos() {
        photoData.value?.forEach {
            val result = storageHelper.deleteFile(it.photoUri)
            Lg.d("Deleting old photo (result = $result): ${it.photoUri}")
        }
    }

    fun onNewPlantName(newCommonName: String?, newScientificName: String?) {
        nullPlantIdsIfInvalid(newCommonName, newScientificName)
        _commonName.value = newCommonName
        _scientificName.value = newScientificName
    }

    private fun nullPlantIdsIfInvalid(newCommonName: String?, newScientificName: String?) {
        vernacularId?.let {
            if (newCommonName != commonName.value)
                vernacularId = null
        }
        taxonId?.let {
            if (newScientificName != scientificName.value)
                taxonId = null
        }
    }

    fun onNewPlantType(newPlantType: Plant.Type) {
        _plantType = newPlantType
        _photoData.value = photoData.value?.map { it.copy(plantType = newPlantType) }
        if (newPlantType != TREE && trunkDiameter.value != null) {
            _trunkDiameter.value = null // Trunk diameter measurement permitted only if plantType = TREE
        }
    }

    fun onPhotoComplete(photoIndex: Int) {
        Lg.d("onPhotoComplete()")
        photoData.value?.let { photoData ->
            if (isPhotoRetake) {
                val oldPhotoUri = photoData[photoIndex].photoUri
                val result = storageHelper.deleteFile(oldPhotoUri)
                Lg.d("Deleting old photo: $oldPhotoUri (Result = $result)")
                val newPhoto = photoData[photoIndex].copy(photoUri = newPhotoUri)
                _photoData.value = photoData.toMutableList().apply { set(photoIndex, newPhoto) }
            } else {
                _photoData.value = photoData.toMutableList().apply {
                    add(PhotoData(plantType, newPhotoType, newPhotoUri, userNickname))
                }
            }
        }
    }

    fun onMeasurementsUpdated(height: Measurement?, diameter: Measurement?, trunkDiameter: Measurement?) {
        _height.value = height
        _diameter.value = diameter
        _trunkDiameter.value = trunkDiameter
    }

    suspend fun savePlantComposite(location: Location) {
        database.withTransaction {
            Lg.d("Saving PlantComposite to database now...")
            var plant = Plant(userId, plantType, commonName.value, scientificName.value, vernacularId, taxonId)
            val plantId = plantRepo.insert(plant)
            plant = plantRepo.get(plantId)
            Lg.d("Saved: $plant")

            savePlantPhotos(plant)
            savePlantMeasurements(plant)
            savePlantLocation(plant, location)
            vernacularId?.let { vernacularRepo.setTagged(it, USED) }
            taxonId?.let { taxonRepo.setTagged(it, USED) }
        }
    }

    private suspend fun savePlantPhotos(plant: Plant) {
        photoData.value?.forEach { (_, photoType, photoUri) ->
            val photoFilename = photoUri.substringAfterLast('/')
            var photo = PlantPhoto(userId, plant.id, photoType, photoFilename)
            val photoId = plantPhotoRepo.insert(photo)
            photo = plantPhotoRepo.get(photoId)
            Lg.d("Saved: $photo")
        }
    }

    private suspend fun savePlantMeasurements(plant: Plant) {
        height.value?.let {
            var heightMeasurement = PlantMeasurement(userId, plant.id, HEIGHT, it.toCm())
            val heightMeasurementId = plantMeasurementRepo.insert(heightMeasurement)
            heightMeasurement = plantMeasurementRepo.get(heightMeasurementId)
            Lg.d("Saved: $heightMeasurement")
        }
        diameter.value?.let {
            var diameterMeasurement = PlantMeasurement(userId, plant.id, DIAMETER, it.toCm())
            val diameterMeasurementId = plantMeasurementRepo.insert(diameterMeasurement)
            diameterMeasurement = plantMeasurementRepo.get(diameterMeasurementId)
            Lg.d("Saved: $diameterMeasurement (id=${diameterMeasurement.id})")
        }
        trunkDiameter.value?.let {
            var trunkDiameterMeasurement = PlantMeasurement(userId, plant.id, TRUNK_DIAMETER, it.toCm())
            val trunkDiameterMeasurementId = plantMeasurementRepo.insert(trunkDiameterMeasurement)
            trunkDiameterMeasurement = plantMeasurementRepo.get(trunkDiameterMeasurementId)
            Lg.d("Saved: $trunkDiameterMeasurement")
        }
    }

    private suspend fun savePlantLocation(plant: Plant, location: Location) {
        var plantLocation = PlantLocation(plant.id, location)
        val plantLocationId = plantLocationRepo.insert(plantLocation)
        plantLocation = plantLocationRepo.get(plantLocationId)
        Lg.d("Saved: $plantLocation")
    }
}