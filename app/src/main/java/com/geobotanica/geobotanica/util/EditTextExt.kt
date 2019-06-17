package com.geobotanica.geobotanica.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

fun EditText.toTrimmedString() = text.toString().trim()
fun EditText.isEmpty() = text.isEmpty()
fun EditText.isNotEmpty() = text.isNotEmpty()

fun EditText.onTextChanged(block: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            block.invoke(s.toString().trim())
        }
        override fun afterTextChanged(s: Editable?) { }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
    } )
}
