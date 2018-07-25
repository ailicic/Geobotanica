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
        private val photoRepo: PhotoRepo,
        private val measurementRepo: MeasurementRepo
): ViewModel() {
    var plantId = 0L    // Field injection of dynamic parameter.
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
        Lg.d("PlantDetailViewModel: init(plantId=$plantId)")
        plant = plantRepo.get(plantId)
        user = switchMap(plant) { plant ->
            plant?.let { userRepo.get(plant.userId) }
        }

        location = map( plantLocationRepo.getLastPlantLocation(plantId) ) { it.location }

        photos = photoRepo.getAllPhotosOfPlant(plantId)

        mainPhoto = photoRepo.getMainPhotoOfPlant(plantId)

        height = measurementRepo.getHeightOfPlant(plantId)
        heightDateText = map(height) { it?.timestamp?.toSimpleDate() ?: "" }

        diameter = measurementRepo.getDiameterOfPlant(plantId)
        diameterDateText = map(diameter) { it?.timestamp?.toSimpleDate() ?: "" }

        trunkDiameter = measurementRepo.getTrunkDiameterOfPlant(plantId)
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

    fun deletePlant() {
        photos.value?.forEach {
            Lg.d("Deleting photo: ${it.fileName}")
            File(it.fileName).delete()
        }

        Lg.d("Deleting plant: ${plant.value!!}")
        plantRepo.delete(plant.value!!)
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