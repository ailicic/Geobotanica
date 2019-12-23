package com.geobotanica.geobotanica.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

inline fun <reified T: Any?> mutableLiveData(initial: T) = MutableLiveData<T>().apply { value = initial }
inline fun <reified T: Any?> liveData(initial: T) = mutableLiveData(initial) as LiveData<T>