package com.geobotanica.geobotanica.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.util.Measurement
import com.geobotanica.geobotanica.util.Units
import kotlinx.android.synthetic.main.compound_edit_measurements.view.*
import kotlinx.android.synthetic.main.dialog_measurements.*

class InputMeasurementsDialog(
        private val titleResId: Int,
        private val plantType: Plant.Type,
        private val height: Measurement?,
        private val diameter: Measurement?,
        private val trunkDiameter: Measurement?,
        private val onNewMeasurements: (height: Measurement?, diameter: Measurement?, trunkDiameter: Measurement?) -> Unit
) : DialogFragment() {

    private lateinit var dialog: AlertDialog
    private lateinit var customView: View // Required for kotlinx synthetic bindings

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        customView = LayoutInflater.from(context).inflate(R.layout.dialog_measurements, null)

        dialog = with(AlertDialog.Builder(activity!!)) {
            setTitle(getString(titleResId))
            setView(customView)
            setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            setPositiveButton(getString(R.string.apply), ::onClickApply)
            create()
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return customView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        measurementsEditView.init(plantType, height, diameter, trunkDiameter)
        bindListeners()
    }

    override fun onStart() {
        super.onStart()
        updateApplyButtonVisiblity()
        useDefaultUnitsIfEmpty()
    }

    private fun useDefaultUnitsIfEmpty() {
        measurementsEditView.setUnits(
            if (height == null) Units.M else null,
            if (diameter == null) Units.M else null
        )
    }

    private fun bindListeners() {
        with(measurementsEditView) {
            heightEditView.onTextChanged(::onMeasurementChanged)
            diameterEditView.onTextChanged(::onMeasurementChanged)
            trunkDiameterEditView.onTextChanged(::onMeasurementChanged)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onMeasurementChanged(text: String) {
        updateApplyButtonVisiblity()
    }

    private fun updateApplyButtonVisiblity() {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = measurementsEditView.isNotEmpty
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickApply(dialog: DialogInterface, which: Int) =
        with(measurementsEditView) { onNewMeasurements(height, diameter, trunkDiameter) }
}

