package com.geobotanica.geobotanica.ui.newplantname

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantNameViewModel @Inject constructor (): ViewModel() {
    var userId = 0L
    var plantType = 0
    var photoUri: String = ""
}