package com.geobotanica.geobotanica.ui.plantdetail

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.UserRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.data.repo.LocationRepo
import com.geobotanica.geobotanica.data.repo.PhotoRepo
import com.geobotanica.geobotanica.data.repo.MeasurementRepo
import javax.inject.Inject


class PlantDetailViewModel @Inject constructor(
        var userRepo: UserRepo,
        var plantRepo: PlantRepo,
        var locationRepo: LocationRepo,
        var photoRepo: PhotoRepo,
        var measurementRepo: MeasurementRepo
): ViewModel() {
    var plantId = 0L    // Must be set externally after instantiation. Very ugly approach to injection of dynamic parameter.
        set(value) {
            field = value
            init()
        }
    // More sophisticated approach to dynamic parameter injection
    // https://brightinventions.pl/blog/android-viewmodel-injections-revisited/

    private lateinit var plant: Plant
    private lateinit var location: Location
    private var photos = emptyList<Photo>()
    private var measurements = emptyList<Measurement>()

    lateinit var user: LiveData<User>

    fun init() {
        user = userRepo.get( plantRepo.get(plantId).userId )
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