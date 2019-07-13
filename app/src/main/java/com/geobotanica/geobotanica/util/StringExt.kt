package com.geobotanica.geobotanica.util

fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.capitalize() }

fun String.replacePrefix(oldPrefix: String, newPrefix: String): String {
    return if (startsWith(oldPrefix))
        replaceFirst(oldPrefix, newPrefix)
    else
        this
}