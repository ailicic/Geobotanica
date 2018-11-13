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


class MeasurementCompoundView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
//        Lg.v("MeasurementCompoundView()")
        inflate(getContext(), R.layout.measurement_compound_view,this)

        unitsSpinner.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View, position: Int, id: Long) {
//                Lg.d("unitsSpinner.onItemSelected(): position=$position, id=$id")
                if (id == Units.FT.ordinal.toLong()) {
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
        val units = unitsSpinner.selectedItemId.toInt()
        return Measurement(value, units).convert(Units.CM).value
    }
}