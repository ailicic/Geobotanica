package com.geobotanica.geobotanica.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer


fun <T> LiveData<T>.observeAfterUnsubscribe(fragment: Fragment, observer: Observer<T>) {
    this.removeObserver(observer)
    this.observe(fragment, observer)
}