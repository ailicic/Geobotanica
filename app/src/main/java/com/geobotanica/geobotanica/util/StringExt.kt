package com.geobotanica.geobotanica.util

import android.annotation.SuppressLint
import java.util.*

@SuppressLint("DefaultLocale")
fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.capitalize() }

fun String.replacePrefix(oldPrefix: String, newPrefix: String): String {
    return if (startsWith(oldPrefix))
        replaceFirst(oldPrefix, newPrefix)
    else
        this
}

fun String?.nullIfBlank(): String? = if (isNullOrBlank()) null else this

fun String?.formatAsMapFilename() = this?.toLowerCase(Locale.US)?.replace(' ', '-')