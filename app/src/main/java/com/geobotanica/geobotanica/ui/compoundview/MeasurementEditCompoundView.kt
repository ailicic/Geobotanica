package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.isVisible
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.util.*
import kotlinx.android.synthetic.main.compound_edit_measurement.view.*


class MeasurementEditCompoundView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    var measurement: Measurement?
        get() {
            return if (isNotEmpty())
                Measurement(value, units)
            else
                null
        }
        set(value) {
            value?.let {
                if (it.units == Units.FT) {
                    val (feet, inches) = it.toFtIn()
                    measurementEditText.setText(feet.toString())
                    measurementInchesEditText.setText(inches.toString())
                    measurementUnitSpinner.setSelection(Units.FT.ordinal)
                } else {
                    measurementEditText.setText(it.value.toString())
                    measurementUnitSpinner.setSelection(it.units.ordinal)
                }
            }
        }

    val value: Float
        get() {
            val value = measurementEditText.text.toString().toFloat()
            return if (measurementInchesEditText.isVisible) {
                val inches = if (measurementInchesEditText.text.isEmpty()) 0F else measurementInchesEditText.text.toString().toFloat() / 12
                value + inches
            } else value

        }

    var units: Units
        get() = Units.values()[measurementUnitSpinner.selectedItemId.toInt()]
        set(value) {
            measurementUnitSpinner.setSelection(value.ordinal)
        }

    init {
        inflate(getContext(), R.layout.compound_edit_measurement,this)
        bindListeners()

    }

    private fun bindListeners() {
        measurementUnitSpinner.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                if (id == Units.FT.ordinal.toLong()) {
                    measurementInchesEditText.isVisible = true
                    inchesText.isVisible = true
                } else {
                    measurementInchesEditText.isVisible = false
                    inchesText.isVisible = false
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) { }
        }
    }

    fun onTextChanged(watcher: (String) -> Unit) {
        measurementEditText.onTextChanged(watcher)
        measurementInchesEditText.onTextChanged(watcher)
    }

    fun isNotEmpty() = measurementEditText.isNotEmpty()
}