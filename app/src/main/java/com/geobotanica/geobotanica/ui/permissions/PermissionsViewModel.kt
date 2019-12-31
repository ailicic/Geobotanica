package com.geobotanica.geobotanica.ui.permissions

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionsViewModel @Inject constructor(): ViewModel() {
    var userId: Long = 0L
}
