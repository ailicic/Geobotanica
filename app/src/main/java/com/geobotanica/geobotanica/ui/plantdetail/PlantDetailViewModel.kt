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

// TODO: Fix crash after delete plant

class PlantDetailViewModel @Inject constructor(
        private var userRepo: UserRepo,
        private var plantRepo: PlantRepo,
        private var plantLocationRepo: PlantLocationRepo,
        private var photoRepo: PhotoRepo,
        private var measurementRepo: MeasurementRepo
): ViewModel() {
    var plantId = 0L    // Must be set externally after instantiation. Very ugly approach to injection of dynamic parameter.
        set(value) {
            field = value
            init()
        }
    // More sophisticated approach to dynamic parameter injection
    // https://brightinventions.pl/blog/android-viewmodel-injections-revisited/

    lateinit var plant: LiveData<Plant>
    lateinit var user: LiveData<User>
    lateinit var location: LiveData<Location>

    lateinit var photos: LiveData<List<Photo>>
    lateinit var mainPhoto: LiveData<Photo>

    lateinit var height: LiveData<Measurement>
    lateinit var heightDateText: LiveData<String>

    lateinit var diameter: LiveData<Measurement>
    lateinit var diameterDateText: LiveData<String>

    lateinit var trunkDiameter: LiveData<Measurement>
    lateinit var trunkDiameterDateText: LiveData<String>

    lateinit var measuredByUser: LiveData<String>
    lateinit var locationText: LiveData<String>
    lateinit var createdDateText: LiveData<String>

    private fun init() {
        Lg.d("PlantDetailViewModel: init()")
        plant = plantRepo.get(plantId)
        user = switchMap(plant) { userRepo.get(it.userId) }
        location = switchMap(plant) { plant ->
            map( plantLocationRepo.getPlantLocation(plant.id) ) { it.location }
        }
        photos = switchMap(plant) { photoRepo.getAllPhotosOfPlant(it.id) }
        mainPhoto = photoRepo.getMainPhotoOfPlant(plantId)

        height = measurementRepo.getHeightOfPlant(plantId)
        heightDateText = map(height) { it?.timestamp?.toSimpleDate() ?: "" }

        diameter = measurementRepo.getDiameterOfPlant(plantId)
        diameterDateText = map(diameter) { it?.timestamp?.toSimpleDate() ?: "" }

        trunkDiameter = measurementRepo.getTrunkDiameterOfPlant(plantId)
        trunkDiameterDateText = map(trunkDiameter) { it?.timestamp?.toSimpleDate() ?: "" }

        measuredByUser = switchMap(height) { height ->
            height?.let {
                map(userRepo.get(height.userId)) { it.nickname }
            } ?: MutableLiveData<String>().apply { value = "" }
        }
        createdDateText = map(plant) { it.timestamp.toSimpleDate() }

        plantRepo.getPlantComposite(plantId).observeForever {
            it?.let { Lg.d("$it") }
        }
    }

    fun deletePlant() {
        photos.observeForever { photos ->
            Lg.d("Deleting ${photos?.size} photos")
            photos?.forEach { File(it.fileName).delete() }
        }
        plant.observeForever {
            it?.let {
                Lg.d("Deleting plant: $it")
                plantRepo.delete(it)
            }
        }
    }
}


// EXAMPLE OF RX -> LIVEDATA CONVERSION IN VIEWMODEL
// https://github.com/googlesamples/android-architecture-components/issues/41
//class UserListViewModel @Inject
//constructor(@NonNull userRepository: UserRepository) : ViewModel() {
//
//    internal val userList: LiveData<Resource<List<User>>>
//
//    init {
//        userList = LiveDataReactiveStreams.fromPublisher(userRepository
//                .getUserList()
//                .subscribeOn(Schedulers.newThread())
//                .observeOn(AndroidSchedulers.mainThread()))
//    }
//}