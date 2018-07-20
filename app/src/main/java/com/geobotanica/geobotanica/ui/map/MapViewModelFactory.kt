package com.geobotanica.geobotanica.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.User
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.data.repo.UserRepo
import javax.inject.Inject

class MapViewModelFactory @Inject constructor(
        private val userRepo: UserRepo,
        private val plantRepo: PlantRepo,
        private val locationService: LocationService
) : ViewModelProvider.Factory {
    var userId = 0L

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(plantRepo, locationService).apply {
            userId = userRepo.insert(User(1,"Guest"))  // Manual field injection of dynamic dependency (better approaches exist)
        } as T
    }
}