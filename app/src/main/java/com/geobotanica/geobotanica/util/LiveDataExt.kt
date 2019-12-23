package com.geobotanica.geobotanica.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

inline fun <reified T: Any?> liveData(initial: T) = MutableLiveData<T>().apply { value = initial } as LiveData<T>