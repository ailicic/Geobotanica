package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.Measurement
import com.geobotanica.geobotanica.util.Units
import kotlinx.android.synthetic.main.measurement_compound_view.view.*


class MeasurementEditView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
//        Lg.v("MeasurementCompoundView()")
        inflate(getContext(), R.layout.measurement_compound_view,this)

        setMeasurementNameText()
        bindListeners()
    }

    private fun setMeasurementNameText() { // Instead of defining custom attribute for text.
        if (this.id == R.id.heightMeasurementView)
            measurementNameText.text = resources.getString(R.string.height)

        if (this.id == R.id.diameterMeasurementView)
            measurementNameText.text = resources.getString(R.string.diameter)

        if (this.id == R.id.trunkDiameterMeasurementView)
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

    fun isEmpty() = measurementEditText.text.isEmpty()

    fun getValue(): Float {
        val value = measurementEditText.text.toString().toFloat()
        return if (measurementInchesEditText.isVisible) {
            val inches = if (measurementInchesEditText.text.isEmpty()) 0F else measurementInchesEditText.text.toString().toFloat() / 12
            value + inches
        } else value
    }

    fun getUnits() = Units.values()[measurementUnitSpinner.selectedItemId.toInt()]

    fun getMeasurement(): Measurement? {
        return if (isEmpty())
            null
        else
            Measurement(getValue(), getUnits())
    }

    fun setMeasurement(measurement: Measurement) {
        if (measurement.units == Units.FT) {
            val (feet, inches) = measurement.toFtIn()
            measurementEditText.setText(feet.toString())
            measurementInchesEditText.setText(inches.toString())
            measurementUnitSpinner.setSelection(Units.FT.ordinal)
        } else {
            measurementInchesEditText.setText(measurement.value.toString())
            measurementUnitSpinner.setSelection(measurement.units.ordinal)
        }
    }
}