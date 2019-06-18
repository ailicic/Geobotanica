package com.geobotanica.geobotanica.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.util.Measurement
import kotlinx.android.synthetic.main.dialog_measurements.*

class EditMeasurementsDialog(
        private val plantType: Plant.Type,
        private val height: Measurement?,
        private val diameter: Measurement?,
        private val trunkDiameter: Measurement?,
        private val onNewMeasurements: (height: Measurement?, diameter: Measurement?, trunkDiameter: Measurement?) -> Unit
) : DialogFragment() {

    private lateinit var dialog: AlertDialog
    private lateinit var customView: View // Required for kotlinx synthetic bindings

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        customView = LayoutInflater.from(context).inflate(R.layout.dialog_measurements, null)

        dialog = with(AlertDialog.Builder(activity!!)) {
            setTitle(getString(R.string.edit_plant_measurements))
            setView(customView)
            setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            setPositiveButton(getString(R.string.apply), ::onClickApply)
            create()
        }
        return dialog
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickApply(dialog: DialogInterface, which: Int) =
            onNewMeasurements(heightEditView.measurement, diameterEditView.measurement, trunkDiameterEditView.measurement)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return customView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        bindListeners()
    }

    private fun initViews() {
        height?.let { heightEditView.measurement = it }
        diameter?.let { diameterEditView.measurement = it }
        trunkDiameter?.let { trunkDiameterEditView.measurement = it }
        if (plantType != Plant.Type.TREE)
            trunkDiameterEditView.isVisible = false
    }

    private fun bindListeners() {
        heightEditView.onTextChanged(::onMeasurementChanged)
        diameterEditView.onTextChanged(::onMeasurementChanged)
        trunkDiameterEditView.onTextChanged(::onMeasurementChanged)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onMeasurementChanged(text: String) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isMeasurementValid()
    }

    private fun isMeasurementValid(): Boolean =
        heightEditView.isNotEmpty() || diameterEditView.isNotEmpty() || trunkDiameterEditView.isNotEmpty()
}
