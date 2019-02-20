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
import android.widget.EditText
import android.widget.Spinner
import androidx.core.view.isVisible


class MeasurementEditView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    lateinit var valueEditText: EditText
    lateinit var unitsSpinner: Spinner
    lateinit var valueInchesEditText: EditText

    init {
//        Lg.v("MeasurementCompoundView()")
        inflate(getContext(), R.layout.measurement_compound_view,this)

        setStaticViewIds()
        bindListeners()
    }

    private fun setStaticViewIds() {
        findViewById<MeasurementEditView>(R.id.heightMeasurementView)?.run {
            valueEditText = editText1.apply { id = R.id.heightValue }
            unitsSpinner = spinner.apply { id = R.id.heightUnits }
            valueInchesEditText = editText2.apply { id = R.id.heightInchesValue }

            textView.text = resources.getString(R.string.height)
        }

        findViewById<MeasurementEditView>(R.id.diameterMeasurementView)?.run {
            valueEditText = editText1.apply { id = R.id.diameterValue }
            unitsSpinner = spinner.apply { id = R.id.diameterUnits }
            valueInchesEditText = editText2.apply { id = R.id.diameterInchesValue }

            textView.text = resources.getString(R.string.diameter)
        }

        findViewById<MeasurementEditView>(R.id.trunkDiameterMeasurementView)?.run {
            valueEditText = editText1.apply { id = R.id.trunkDiamValue }
            unitsSpinner = spinner.apply { id = R.id.trunkDiamUnits }
            valueInchesEditText = editText2.apply { id = R.id.trunkDiamInchesValue }

            textView.text = resources.getString(R.string.trunk_diameter)
        }
    }

    private fun bindListeners() {
        spinner.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                //                Lg.d("unitsSpinner.onItemSelected(): position=$position, id=$id")
                if (id == Units.FT.ordinal.toLong()) {
                    valueInchesEditText.isVisible = true
                    inchesTextView.isVisible = true
                } else {
                    valueInchesEditText.isVisible = false
                    inchesTextView.isVisible = false
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                Lg.d("onNothingSelected()")
            }
        }
    }

    fun isEmpty() = valueEditText.text.isEmpty()

    fun getValue(): Float {
        val value = valueEditText.text.toString().toFloat()
        return if (valueInchesEditText.isVisible) {
            val inches = if (valueInchesEditText.text.isEmpty()) 0F else valueInchesEditText.text.toString().toFloat() / 12
            value + inches
        } else value
    }

    fun getUnits() = Units.values()[unitsSpinner.selectedItemId.toInt()]

    fun getMeasurement(): Measurement? {
        return if (isEmpty())
            null
        else
            Measurement(getValue(), getUnits())
    }

    fun setMeasurement(measurement: Measurement) {
        if (measurement.units == Units.FT) {
            val (feet, inches) = measurement.toFtIn()
            valueEditText.setText(feet.toString())
            valueInchesEditText.setText(inches.toString())
            unitsSpinner.setSelection(Units.FT.ordinal)
        } else {
            valueEditText.setText(measurement.value.toString())
            unitsSpinner.setSelection(measurement.units.ordinal)
        }
    }
}