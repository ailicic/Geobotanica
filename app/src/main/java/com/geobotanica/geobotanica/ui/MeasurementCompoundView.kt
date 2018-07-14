package com.geobotanica.geobotanica.ui

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.measurement_compound_view.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import com.geobotanica.geobotanica.data.entity.Measurement


class MeasurementCompoundView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        Lg.v("MeasurementCompoundView()")
        inflate(getContext(), R.layout.measurement_compound_view,this)

        unitsSpinner.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View, position: Int, id: Long) {
                Lg.d("unitsSpinner.onItemSelected(): position=$position, id=$id")
                if (id == Measurement.Unit.FT.ordinal.toLong()) {
                    inchesEditText.visibility = View.VISIBLE
                    inchesTextView.visibility = View.VISIBLE
                } else {
                    inchesEditText.visibility = View.INVISIBLE
                    inchesTextView.visibility = View.INVISIBLE
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                Lg.d("onNothingSelected()")
            }
        }
    }

    fun getInCentimeters(): Float {
        val value = editText.text.toString().toFloatOrNull() ?: 0F
        return when (unitsSpinner.selectedItemId.toInt()) {
            Measurement.Unit.CM.ordinal -> value
            Measurement.Unit.M.ordinal -> 100 * value
            Measurement.Unit.IN.ordinal -> 2.54F * value
            Measurement.Unit.FT.ordinal -> {
                val inches = inchesEditText.text.toString().toFloatOrNull() ?: 0F
                2.54F * (12 * value + inches)
            }
            else -> { // TODO: Maybe use enum in spinner to avoid else?
                Lg.d("getInCentimeters(): Error"); return 0F
            }
        }
    }




}