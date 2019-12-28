package com.geobotanica.geobotanica.util

import android.view.View
import android.widget.AdapterView
import android.widget.Spinner

fun Spinner.onItemSelected(callback: (Int) -> Unit) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, rowId: Long) {
            callback(rowId.toInt())
        }
        override fun onNothingSelected(parentView: AdapterView<*>) { }
    }
}