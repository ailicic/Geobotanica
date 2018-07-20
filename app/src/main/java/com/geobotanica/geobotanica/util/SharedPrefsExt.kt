package com.geobotanica.geobotanica.util

import android.content.SharedPreferences


fun SharedPreferences.Editor.putDouble(key: String, value: Double) {
    putLong(key, value.toRawBits())
}

fun SharedPreferences.getDouble(key: String, defaultValue: Double) =
        Double.fromBits( getLong(key, defaultValue.toRawBits()) )