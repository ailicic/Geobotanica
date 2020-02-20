package com.geobotanica.geobotanica.util

import androidx.work.OneTimeWorkRequest

fun OneTimeWorkRequest.Builder.addTags(vararg tag: String): OneTimeWorkRequest.Builder {
    tag.forEach { addTag(it) }
    return this
}