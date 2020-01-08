package com.geobotanica.geobotanica.util

import android.os.Bundle
import com.geobotanica.geobotanica.ui.BaseFragment


inline fun <reified T: Any?> BaseFragment.getNullableFromBundle(key: String): T? =
    this.arguments?.getValue<T>(key)

inline fun <reified T: Any> BaseFragment.getFromBundle(key: String, default: T = getDefault()): T =
        getNullableFromBundle(key) ?: default

inline fun <reified T: Any?> Bundle.getValue(key: String): T? {
    return when (T::class) {
        Boolean::class -> { this.getBoolean(key) as T?}
        Int::class -> {
            val value = this.getInt(key) as T?
            return if (value == 0) null else value // Ensure null is returned if absent
        }
        Long::class -> {
            val value = this.getLong(key) as T?
            return if (value == 0L) null else value // Ensure null is returned if absent
        }
        Float::class -> {
            val value = this.getFloat(key) as T?
            return if (value == 0F) null else value // Ensure null is returned if absent
        }
        Double::class -> {
            val value = this.getDouble(key) as T?
            return if (value == 0.0) null else value // Ensure null is returned if absent
        }
        String::class -> { this.getString(key) as T? }
        Measurement::class -> { this.getSerializable(key) as T? }
        else -> throw IllegalArgumentException("$key is of unknown type")
    }
}

inline fun <reified T: Any>getDefault(): T {
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