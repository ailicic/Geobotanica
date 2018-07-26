package com.geobotanica.geobotanica.util

import android.os.Bundle
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.util.SharedPrefsExt.get
import com.geobotanica.geobotanica.util.SharedPrefsExt.getSharedPrefs

object NavBundleExt {

    inline fun <reified T: Any> BaseFragment.getFromBundleOrPrefs(key: String, default: T): T =
        this.arguments?.getValue(key) ?: getSharedPrefs(this.sharedPrefsKey).get(key, default)

    inline fun <reified T: Any> Bundle.getValue(key: String): T {
        return when (T::class) {
            Boolean::class -> { this.getBoolean(key) as T}
            Int::class -> { this.getInt(key) as T}
            Long::class -> { this.getLong(key) as T}
            Float::class -> { this.getFloat(key) as T}
            Double::class -> { this.getDouble(key) as T}
            String::class -> { this.getString(key) as T}
            else -> throw IllegalArgumentException("Type for $key unknown")
        }
    }
}