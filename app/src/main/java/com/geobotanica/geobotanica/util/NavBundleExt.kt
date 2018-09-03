package com.geobotanica.geobotanica.util

import android.os.Bundle
import com.geobotanica.geobotanica.ui.BaseFragment

object NavBundleExt {

    inline fun <reified T: Any?> BaseFragment.getNullableFromBundle(key: String): T? =
        this.arguments?.getValue<T>(key)

    inline fun <reified T: Any> BaseFragment.getFromBundle(key: String, default: T = createDefault()): T =
        this.arguments?.getValue<T>(key) ?: default

    inline fun <reified T: Any?> Bundle.getValue(key: String): T? {
        return when (T::class) {
            Boolean::class -> { this.getBoolean(key) as T?}
            Int::class -> { this.getInt(key) as T? }
            Long::class -> { this.getLong(key) as T? }
            Float::class -> { this.getFloat(key) as T? }
            Double::class -> { this.getDouble(key) as T? }
            String::class -> { this.getString(key) as T? }
            else -> throw IllegalArgumentException("$key is of unknown type")
        }
    }

    inline fun <reified T: Any>createDefault(): T {
        return when (T::class) {
            Boolean::class -> false as T
            Int::class -> 0 as T
            Long::class -> 0L as T
            Float::class -> 0F as T
            Double::class -> 0 as T
            String::class -> "" as T
            else -> throw IllegalArgumentException("${T::class.java.name} is of unknown type")
        }
    }
}


// Restructured since nav bundle is always provided to fragment, even after process death
// https://medium.com/google-developers/viewmodels-persistence-onsaveinstancestate-restoring-ui-state-and-loaders-fc7cc4a6c090
//    inline fun <reified T: Any> BaseFragment.getFromBundleOrPrefs(key: String, default: T? = null): T {
//        return this.arguments?.getValue<T>(key)
//                ?: getSharedPrefs(this.sharedPrefsKey).get(key, default ?: createDefault())
//    }