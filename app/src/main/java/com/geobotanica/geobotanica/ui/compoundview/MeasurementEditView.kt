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


class MeasurementEditView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    var measurement: Measurement?
        get() {
            return if (isEmpty())
                null
            else
                Measurement(value, units)
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

    val units: Units
        get() = Units.values()[measurementUnitSpinner.selectedItemId.toInt()]

    init {
//        Lg.v("MeasurementCompoundView()")
        inflate(getContext(), R.layout.compound_edit_measurement,this)
        setStaticViewIds()

        setMeasurementNameText()
        bindListeners()
    }

    private fun setStaticViewIds() { // Required for restoring state from bundle (e.g. use back to return here)
        if (this.id == R.id.heightEditView) {
            measurementEditText.id = R.id.heightValue
            measurementUnitSpinner.id = R.id.heightUnits
            measurementInchesEditText.id = R.id.heightInchesValue
        }

        if (this.id == R.id.diameterEditView) {
            measurementEditText.id = R.id.diameterValue
            measurementUnitSpinner.id = R.id.diameterUnits
            measurementInchesEditText.id = R.id.diameterInchesValue
        }

        if (this.id == R.id.trunkDiameterEditView) {
            measurementEditText.id = R.id.trunkDiamValue
            measurementUnitSpinner.id = R.id.trunkDiamUnits
            measurementInchesEditText.id = R.id.trunkDiamInchesValue
        }
    }

    private fun setMeasurementNameText() { // Instead of defining custom attribute for text.
        if (this.id == R.id.heightEditView)
            measurementNameText.text = resources.getString(R.string.height)

        if (this.id == R.id.diameterEditView)
            measurementNameText.text = resources.getString(R.string.diameter)

        if (this.id == R.id.trunkDiameterEditView)
            measurementNameText.text = resources.getString(R.string.trunk_diameter)
    }

    private fun bindListeners() {
        measurementUnitSpinner.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
//                Lg.d("unitsSpinner.onItemSelected(): position=$position, id=$id")
                if (id == Units.FT.ordinal.toLong()) {
                    measurementInchesEditText.isVisible = true
                    inchesText.isVisible = true
                } else {
                    measurementInchesEditText.isVisible = false
                    inchesText.isVisible = false
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                Lg.d("onNothingSelected()")
            }
        }
    }

    fun onTextChanged(watcher: (String) -> Unit) {
        measurementEditText.onTextChanged(watcher)
        measurementInchesEditText.onTextChanged(watcher)
    }

    fun isEmpty() = measurementEditText.isEmpty()

    fun isNotEmpty() = measurementEditText.isNotEmpty()
}