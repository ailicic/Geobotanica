package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.isVisible
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.util.Measurement
import kotlinx.android.synthetic.main.compound_edit_measurement.view.*
import kotlinx.android.synthetic.main.compound_edit_measurements.view.*


class MeasurementsEditCompoundView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    private lateinit var plantType: Plant.Type

    val height
        get() = heightEditView.measurement
    val diameter
        get() = diameterEditView.measurement
    val trunkDiameter
        get() = trunkDiameterEditView.measurement

    val isNotEmpty: Boolean
        get() {
            return heightEditView.isNotEmpty() || diameterEditView.isNotEmpty() ||
                ( plantType == Plant.Type.TREE && trunkDiameterEditView.isNotEmpty() )
        }

    init {
        inflate(getContext(), R.layout.compound_edit_measurements,this)
        setMeasurementNameText()
//        setStaticViewIds()
    }

    fun init(
            plantType: Plant.Type,
            height: Measurement? = null,
            diameter: Measurement? = null,
            trunkDiameter: Measurement? = null
    ) {
        this.plantType = plantType
        if (plantType != Plant.Type.TREE)
            trunkDiameterEditView.isVisible = false

        heightEditView.measurement = height
        diameterEditView.measurement = diameter
        trunkDiameterEditView.measurement = trunkDiameter
    }

    private fun setMeasurementNameText() { // Instead of defining custom attribute for text.
        heightEditView.measurementNameText.text = resources.getString(R.string.height)
        diameterEditView.measurementNameText.text = resources.getString(R.string.diameter)
        trunkDiameterEditView.measurementNameText.text = resources.getString(R.string.trunk_diameter)
    }

//    private fun setStaticViewIds() { // Required for restoring state from bundle (e.g. use back to return here)
//        with(heightEditView) {
//            measurementEditText.id = R.id.heightValue
//            measurementUnitSpinner.id = R.id.heightUnits
//            measurementInchesEditText.id = R.id.heightInchesValue
//        }
//
//        with(diameterEditView) {
//            measurementEditText.id = R.id.diameterValue
//            measurementUnitSpinner.id = R.id.diameterUnits
//            measurementInchesEditText.id = R.id.diameterInchesValue
//        }
//
//        with(trunkDiameterEditView) {
//            measurementEditText.id = R.id.trunkDiamValue
//            measurementUnitSpinner.id = R.id.trunkDiamUnits
//            measurementInchesEditText.id = R.id.trunkDiamInchesValue
//        }
//    }
}