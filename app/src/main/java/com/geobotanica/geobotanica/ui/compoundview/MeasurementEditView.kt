package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.Units
import com.geobotanica.geobotanica.util.Measurement
import kotlinx.android.synthetic.main.measurement_compound_view.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.core.view.isVisible


class MeasurementEditView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
//        Lg.v("MeasurementCompoundView()")
        inflate(getContext(), R.layout.measurement_compound_view,this)

        unitsSpinner.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
//                Lg.d("unitsSpinner.onItemSelected(): position=$position, id=$id")
                if (id == Units.FT.ordinal.toLong()) {
                    inchesEditText.isVisible = true
                    inchesTextView.isVisible = true
                } else {
                    inchesEditText.isVisible = false
                    inchesTextView.isVisible = false
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                Lg.d("onNothingSelected()")
            }
        }
    }

    fun isEmpty() = editText.text.isEmpty()

    fun getValue(): Float {
        val value = editText.text.toString().toFloat()
        return if (inchesEditText.isVisible) {
            val inches = if (inchesEditText.text.isEmpty()) 0F else inchesEditText.text.toString().toFloat() / 12
            value + inches
        } else value
    }

    fun getUnits() = Units.values()[unitsSpinner.selectedItemId.toInt()]

    fun getMeasurement(): Measurement = Measurement(getValue(), getUnits())

    fun setMeasurement(measurement: Measurement) {
        if (measurement.units == Units.FT) {
            val (feet, inches) = measurement.toFtIn()
            editText.setText(feet.toString())
            inchesEditText.setText(inches.toString())
            unitsSpinner.setSelection(Units.FT.ordinal)
        } else {
            editText.setText(measurement.value.toString())
            unitsSpinner.setSelection(measurement.units.ordinal)
        }
    }
}