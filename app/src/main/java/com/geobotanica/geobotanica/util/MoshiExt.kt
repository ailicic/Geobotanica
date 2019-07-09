package com.geobotanica.geobotanica.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi

inline fun <reified T> Moshi.adapter(): JsonAdapter<T> = adapter(T::class.java)