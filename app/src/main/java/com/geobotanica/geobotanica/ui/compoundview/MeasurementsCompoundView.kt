package com.geobotanica.geobotanica.ui.compoundview

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.ui.dialog.EditMeasurementsDialog
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.Measurement
import com.geobotanica.geobotanica.util.dpToPixels
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.compound_measurements.view.*

class MeasurementsCompoundView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    lateinit var onNewMeasurementsCallback: (Measurement?, Measurement?, Measurement?) -> Unit

    private var height: Measurement? = null
    private var diameter: Measurement? = null
    private var trunkDiameter: Measurement? = null
    private lateinit var plantType: LiveData<Plant.Type>

    init {
        inflate(getContext(), R.layout.compound_measurements, this)
        editMeasurementsButton.setOnClickListener { showEditMeasurementsDialog() }
    }

    fun init(height: Measurement?, diameter: Measurement?, trunkDiameter: Measurement?, plantType: LiveData<Plant.Type>) {
        updateMeasurements(height, diameter, trunkDiameter)

        this.plantType = plantType
        val lifecycleOwner = (context as FragmentActivity).fragment.viewLifecycleOwner
        plantType.observe(lifecycleOwner, Observer {
            if (it != Plant.Type.TREE)
                onNewMeasurements(this.height, this.diameter, null)
        })
    }

    private fun showEditMeasurementsDialog() {
        EditMeasurementsDialog(
                plantType.value!!,
                height,
                diameter,
                trunkDiameter,
                ::onNewMeasurements
        ).show((context as FragmentActivity).supportFragmentManager, "tag")
    }

    private fun onNewMeasurements(height: Measurement?, diameter: Measurement?, trunkDiameter: Measurement?) {
        updateMeasurements(height, diameter, trunkDiameter)
        onNewMeasurementsCallback(height, diameter, trunkDiameter)
    }

    private fun updateMeasurements(height: Measurement?, diameter: Measurement?, trunkDiameter: Measurement?) {
        this.height = height
        this.diameter = diameter
        this.trunkDiameter = trunkDiameter

        updateMeasurementsText()
        updateMeasurementsLayout()
        updateMeasurementsVisibility()
    }

    private fun updateMeasurementsLayout() {
        var measurementCount = 0

        height?.let { ++measurementCount }
        diameter?.let { ++measurementCount }
        trunkDiameter?.let { ++measurementCount }

        val buttonLayoutParams = editMeasurementsButton.layoutParams as LayoutParams
        if (measurementCount > 2)
            buttonLayoutParams.topMargin = dpToPixels(20)
        else
            buttonLayoutParams.topMargin = dpToPixels(8)
        editMeasurementsButton.layoutParams = buttonLayoutParams
    }

    private fun updateMeasurementsText() {
        heightText.text = height?.toHeightString()
        diameterText.text = diameter?.toDiameterString()
        trunkDiameterText.text = trunkDiameter?.toTrunkDiameterString()
    }

    private fun updateMeasurementsVisibility() {
        heightText.isVisible = height != null
        diameterText.isVisible = diameter != null
        trunkDiameterText.isVisible = trunkDiameter != null
    }
}
