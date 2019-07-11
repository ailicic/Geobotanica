package com.geobotanica.geobotanica.util

fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.capitalize() }