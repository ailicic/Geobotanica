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
import com.geobotanica.geobotanica.util.isNotEmpty
import com.geobotanica.geobotanica.util.onTextChanged
import com.geobotanica.geobotanica.util.toTrimmedString
import kotlinx.android.synthetic.main.dialog_plant_name.*

class EditPlantNameDialog(
        val commonName: String,
        val scientificName: String,
        val onNewPlantName: (commonName: String, scientificName: String) -> Unit
) : DialogFragment() {

    private lateinit var dialog: AlertDialog
    private lateinit var customView: View // Required for kotlinx synthetic bindings

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        customView = LayoutInflater.from(context).inflate(R.layout.dialog_plant_name, null)

        dialog = with(AlertDialog.Builder(activity!!)) {
            setTitle(getString(R.string.edit_plant_name))
            setView(customView)
            setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            setPositiveButton(getString(R.string.apply), ::onClickOk)
            create()
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return customView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        commonNameEditText.setText(commonName)
        commonNameEditText.setSelection(commonName.length)
        scientificNameEditText.setText(scientificName)
        bindListeners()
    }

    private fun bindListeners() {
        commonNameEditText.onTextChanged(::onCommonEditTextChanged)
        resetCommonButton.setOnClickListener(::onClickResetCommon)
        scientificNameEditText.onTextChanged(::onScientificEditTextChanged)
        resetScientificButton.setOnClickListener(::onResetScientificName)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickOk(dialog: DialogInterface, which: Int) =
        onNewPlantName(commonNameEditText.toTrimmedString(), scientificNameEditText.toTrimmedString())

    private fun onCommonEditTextChanged(editText: String) {
        resetCommonButton.isVisible = editText != commonName
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isPlantNameValid()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickResetCommon(view: View) {
        commonNameEditText.setText(commonName)
        commonNameEditText.setSelection(commonName.length)
    }

    private fun onScientificEditTextChanged(editText: String) {
        resetScientificButton.isVisible = editText != scientificName
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isPlantNameValid()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onResetScientificName(view: View) {
        scientificNameEditText.setText(scientificName)
        scientificNameEditText.setSelection(scientificName.length)
    }

    private fun isPlantNameValid() = commonNameEditText.isNotEmpty() || scientificNameEditText.isNotEmpty()
}