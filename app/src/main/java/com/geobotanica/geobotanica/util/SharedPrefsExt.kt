package com.geobotanica.geobotanica.util

import android.content.SharedPreferences
import com.geobotanica.geobotanica.ui.BaseFragment


object SharedPrefsExt {

    inline fun <reified T: Any> SharedPreferences.get(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is Boolean -> { getBoolean(key, defaultValue) as T}
            is Int -> { getInt(key, defaultValue)  as T}
            is Long -> { getLong(key, defaultValue)  as T}
            is Float -> { getFloat(key, defaultValue)  as T}
            is Double -> { getDouble(key, defaultValue)  as T}
            is String -> { getString(key, defaultValue)  as T}
            else -> throw IllegalArgumentException("$key is of unknown type")
        }
    }

    fun BaseFragment.putSharedPrefs(vararg pairs: Pair<String, Any>) {
        sharedPrefs.edit().run {
            for ((key, value) in pairs) {
                when (value) {
                    is Boolean -> { putBoolean(key, value) }
                    is Int -> { putInt(key, value) }
                    is Long -> { putLong(key, value) }
                    is Float -> { putFloat(key, value) }
                    is Double -> { putDouble(key, value) }
                    is String -> { putString(key, value) }
                    else -> throw IllegalArgumentException("$key is of unknown type")
                }
            }
            apply()
        }
    }

    fun SharedPreferences.Editor.putDouble(key: String, value: Double): SharedPreferences.Editor =
            putLong(key, value.toRawBits())

    fun SharedPreferences.getDouble(key: String, defaultValue: Double) =
        Double.fromBits( getLong(key, defaultValue.toRawBits()) )
}