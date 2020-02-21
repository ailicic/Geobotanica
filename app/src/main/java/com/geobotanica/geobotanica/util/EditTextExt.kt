@file:Suppress("unused")

package com.geobotanica.geobotanica.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

fun EditText.toTrimmedString() = text.toString().trim()
fun EditText.isEmpty() = text.isEmpty()
fun EditText.isNotEmpty() = text.isNotEmpty()
fun EditText.nullIfBlank() = if (text.isBlank()) null else toTrimmedString()

inline fun EditText.onTextChanged(crossinline block: (String) -> Unit): TextWatcher {
    val watcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            block.invoke(s.toString().trim())
        }
        override fun afterTextChanged(s: Editable?) { }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
    }
    addTextChangedListener(watcher)
    return watcher
}
