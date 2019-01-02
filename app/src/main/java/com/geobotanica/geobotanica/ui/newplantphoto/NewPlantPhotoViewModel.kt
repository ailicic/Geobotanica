package com.geobotanica.geobotanica.ui.newplantphoto

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.entity.Plant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantPhotoViewModel @Inject constructor (): ViewModel() {
    var userId = 0L
    var plantType = Plant.Type.TREE
    var photoUri: String = ""
    var oldPhotoUri: String = ""
}