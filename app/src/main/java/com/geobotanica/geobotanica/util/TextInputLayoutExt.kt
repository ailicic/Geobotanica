package com.geobotanica.geobotanica.util

import com.google.android.material.textfield.TextInputLayout


fun TextInputLayout.toTrimmedString() = editText!!.text.toString().trim()
fun TextInputLayout.isEmpty() = editText!!.text.isEmpty()
fun TextInputLayout.isNotEmpty() = editText!!.text.isNotEmpty()