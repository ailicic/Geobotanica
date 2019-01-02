package com.geobotanica.geobotanica.ui.newplantmeasurement

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.util.Measurement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantMeasurementViewModel @Inject constructor() : ViewModel() {
    var userId = 0L
    lateinit var plantType: Plant.Type
    lateinit var photoUri: String
    var commonName: String? = null
    var latinName: String? = null
    var heightMeasurement: Measurement? = null
    var diameterMeasurement: Measurement? = null
    var trunkDiameterMeasurement: Measurement? = null
}