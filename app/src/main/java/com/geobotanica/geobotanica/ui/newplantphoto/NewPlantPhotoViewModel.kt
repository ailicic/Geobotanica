package com.geobotanica.geobotanica.ui.newplantphoto

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantPhotoViewModel @Inject constructor (): ViewModel() {
    var userId = 0L
    var plantType = 0
    var photoUri: String = ""
    var oldPhotoUri: String = ""
}