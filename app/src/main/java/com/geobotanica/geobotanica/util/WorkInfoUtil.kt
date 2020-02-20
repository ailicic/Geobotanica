package com.geobotanica.geobotanica.util

import androidx.work.WorkInfo
import androidx.work.WorkInfo.State.*

val List<WorkInfo>?.isRunning: Boolean get() = this?.any { it.state == RUNNING } ?: false
val List<WorkInfo>?.isSuccessful: Boolean get() = this?.all { it.state == SUCCEEDED } ?: false
val List<WorkInfo>?.isCancelled: Boolean get() = this?.any { it.state == CANCELLED } ?: false
val List<WorkInfo>?.isFailed: Boolean get() = this?.any { it.state == FAILED } ?: false