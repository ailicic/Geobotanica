package com.geobotanica.geobotanica.util

import android.util.TypedValue
import android.view.View

fun View.getString(resId: Int): String = context.resources.getString(resId)

fun View.dpToPixels(dp: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
