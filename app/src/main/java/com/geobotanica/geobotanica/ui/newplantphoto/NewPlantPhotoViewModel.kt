package com.geobotanica.geobotanica.ui.newplantphoto

import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.util.Lg
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPlantPhotoViewModel @Inject constructor (): ViewModel() {
    var userId = 0L
    var currentSessionId = 0L // = Timestamp of when session began
    var photoUri: String = ""
    private var lastSessionId = 0L

    fun deleteLastPhoto() {
        if (currentSessionId == lastSessionId)
            Lg.d("Deleting old photo: photoUri (result=${File(photoUri).delete()}")
        lastSessionId = currentSessionId
    }
}