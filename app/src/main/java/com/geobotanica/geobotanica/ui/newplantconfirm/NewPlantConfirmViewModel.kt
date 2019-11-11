package com.geobotanica.geobotanica.ui.newplantconfirm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.withTransaction
import com.geobotanica.geobotanica.android.location.Location
import com.geobotanica.geobotanica.data.GbDatabase
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.entity.PlantMeasurement.Type.*
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.data_taxa.repo.TaxonRepo
import com.geobotanica.geobotanica.data_taxa.repo.VernacularRepo
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag.USED
import com.geobotanica.geobotanica.ui.viewpager.PhotoData
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.Measurement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantConfirmViewModel @Inject constructor (
        private val database: GbDatabase,
        private val userRepo: UserRepo,
        private val plantRepo: PlantRepo,
        private val plantLocationRepo: PlantLocationRepo,
        private val plantPhotoRepo: PlantPhotoRepo,
        private val plantMeasurementRepo: PlantMeasurementRepo,
        private val taxonRepo: TaxonRepo,
        private val vernacularRepo: VernacularRepo
) : ViewModel() {

    var userId = 0L

    val plantType = MutableLiveData<Plant.Type>()
    val commonName = MutableLiveData<String>() // Used for xml data-binding
    val scientificName = MutableLiveData<String>() // Used for xml data-binding
    var taxonId: Long? = null
    var vernacularId: Long? = null
    var height = MutableLiveData<Measurement>() // Used for xml data-binding
    var diameter = MutableLiveData<Measurement>() // Used for xml data-binding
    var trunkDiameter = MutableLiveData<Measurement>() // Used for xml data-binding
    var location: Location? = null
    var photoUri: String = ""
    val photoData = mutableListOf<PhotoData>()

    init {
        plantType.observeForever {
            if (it != Plant.Type.TREE && trunkDiameter.value != null) {
                trunkDiameter.value = null
            }
        }
    }

    suspend fun initPhotoData() {
        photoData.clear()
        photoData.add(PhotoData(PlantPhoto.Type.COMPLETE, photoUri, getUserNickname()))
    }

    suspend fun getUserNickname(): String = userRepo.get(userId).nickname

    fun onNewPlantName(newCommonName: String?, newScientificName: String?) {
        nullPlantIdsIfInvalid(newCommonName, newScientificName)
        commonName.value = newCommonName
        scientificName.value = newScientificName
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

    fun onMeasurementsUpdated(height: Measurement?, diameter: Measurement?, trunkDiameter: Measurement?) {
        this.height.value = height
        this.diameter.value = diameter
        this.trunkDiameter.value = trunkDiameter
    }

    suspend fun savePlantComposite() {
        database.withTransaction {
            Lg.d("Saving PlantComposite to database now...")
            val plant = Plant(userId, plantType.value!!, commonName.value, scientificName.value, vernacularId, taxonId)
            plant.id = plantRepo.insert(plant)
            Lg.d("Saved: $plant (id=${plant.id})")

            savePlantPhotos(plant)
            savePlantMeasurements(plant)
            savePlantLocation(plant)
            vernacularId?.let { vernacularRepo.setTagged(it, USED) }
            taxonId?.let { taxonRepo.setTagged(it, USED) }
        }
    }

    private suspend fun savePlantPhotos(plant: Plant) {
        photoData.forEach { (photoType, photoUri) ->
            val photoFilename = photoUri.substringAfterLast('/')
            val photo = PlantPhoto(userId, plant.id, photoType, photoFilename)
            photo.id = plantPhotoRepo.insert(photo)
            Lg.d("Saved: $photo (id=${photo.id})")
        }
    }

    private suspend fun savePlantMeasurements(plant: Plant) {
        height.value?.let {
            val heightMeasurement = PlantMeasurement(userId, plant.id, HEIGHT, it.toCm())
            heightMeasurement.id = plantMeasurementRepo.insert(heightMeasurement)
            Lg.d("Saved: $heightMeasurement (id=${heightMeasurement.id})")
        }
        diameter.value?.let {
            val diameterMeasurement = PlantMeasurement(userId, plant.id, DIAMETER, it.toCm())
            diameterMeasurement.id = plantMeasurementRepo.insert(diameterMeasurement)
            Lg.d("Saved: $diameterMeasurement (id=${diameterMeasurement.id})")
        }
        trunkDiameter.value?.let {
            val trunkDiameterMeasurement = PlantMeasurement(userId, plant.id, TRUNK_DIAMETER, it.toCm())
            trunkDiameterMeasurement.id = plantMeasurementRepo.insert(trunkDiameterMeasurement)
            Lg.d("Saved: $trunkDiameterMeasurement (id=${trunkDiameterMeasurement.id})")
        }
    }

    private suspend fun savePlantLocation(plant: Plant) {
        val plantLocation = PlantLocation(plant.id, location!!)
        plantLocation.id = plantLocationRepo.insert(plantLocation)
        Lg.d("Saved: $plantLocation (id=${plantLocation.id})")
    }
}