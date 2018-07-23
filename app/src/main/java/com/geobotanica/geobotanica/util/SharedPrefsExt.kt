package com.geobotanica.geobotanica.util

import android.content.SharedPreferences

object SharedPrefsExt {
    fun SharedPreferences.put(map: Map<String, Any>) {
        this.edit().run {
            for( (key, value) in map)
                put(key, value)
            apply()
        }
    }

    inline fun <reified T: Any> SharedPreferences.Editor.put(key: String, value: T) {
        when (value) {
            is Boolean -> { this.putBoolean(key, value) }
            is Int -> { this.putInt(key, value) }
            is Long -> { this.putLong(key, value) }
            is Float -> { this.putFloat(key, value) }
            is Double -> { this.putDouble(key, value) }
            is String -> { this.putString(key, value) }
        }
    }

    inline fun <reified T: Any> SharedPreferences.get(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is Boolean -> { this.getBoolean(key, defaultValue) as T}
            is Int -> { this.getInt(key, defaultValue)  as T}
            is Long -> { this.getLong(key, defaultValue)  as T}
            is Float -> { this.getFloat(key, defaultValue)  as T}
            is Double -> { this.getDouble(key, defaultValue)  as T}
            is String -> { this.getString(key, defaultValue)  as T}
            else -> throw IllegalArgumentException("")
        }
    }


    fun SharedPreferences.Editor.putDouble(key: String, value: Double) {
        putLong(key, value.toRawBits())
    }

    fun SharedPreferences.getDouble(key: String, defaultValue: Double) =
            Double.fromBits( getLong(key, defaultValue.toRawBits()) )

}