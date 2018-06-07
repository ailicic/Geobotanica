package com.geobotanica.geobotanica.util

import android.util.Log

/**
 * A simple log class based on android.util.Log with a constant tag.
 */

object Lg {
    private const val tag = "Gb"
    fun v(msg: String) = Log.v(tag, msg)
    fun d(msg: String) = Log.d(tag, msg)
    fun i(msg: String) = Log.i(tag, msg)
    fun w(msg: String) = Log.w(tag, msg)
    fun e(msg: String) = Log.e(tag, msg)
}
