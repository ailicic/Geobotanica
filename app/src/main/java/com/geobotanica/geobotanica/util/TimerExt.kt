package com.geobotanica.geobotanica.util

import java.util.TimerTask
import java.util.Timer

inline fun Timer.schedule(delay: Long, crossinline task: () -> Unit): Timer {
    this.schedule(
            object : TimerTask() {
                override fun run() { task.invoke() }
            },
            delay)
    return this
}