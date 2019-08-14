package com.geobotanica.geobotanica.util

import com.google.android.material.textfield.TextInputLayout

fun TextInputLayout.isTextEmpty() = editText!!.isEmpty()