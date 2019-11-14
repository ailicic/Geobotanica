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
import com.geobotanica.geobotanica.util.isNotEmpty
import com.geobotanica.geobotanica.util.nullIfBlank
import com.geobotanica.geobotanica.util.onTextChanged
import kotlinx.android.synthetic.main.dialog_plant_name.*

class EditPlantNameDialog(
        private val commonName: String,
        private val scientificName: String,
        private val onNewPlantName: (commonName: String?, scientificName: String?) -> Unit
) : DialogFragment() {

    private lateinit var dialog: AlertDialog
    private lateinit var customView: View // Required for kotlinx synthetic bindings

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        customView = LayoutInflater.from(context).inflate(R.layout.dialog_plant_name, null)

        dialog = with(AlertDialog.Builder(activity!!)) {
            setTitle(getString(R.string.edit_plant_name))
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
        initViews()
        bindListeners()
    }

    private fun initViews() {
        commonNameEditText.setText(commonName)
        commonNameEditText.setSelection(commonName.length)
        scientificNameEditText.setText(scientificName)
    }

    private fun bindListeners() {
        commonNameEditText.onTextChanged(::onCommonEditTextChanged)
        scientificNameEditText.onTextChanged(::onScientificEditTextChanged)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickApply(dialog: DialogInterface, which: Int) {
        onNewPlantName(commonNameEditText.nullIfBlank(), scientificNameEditText.nullIfBlank())
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onCommonEditTextChanged(editText: String) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isPlantNameValid()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onScientificEditTextChanged(editText: String) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isPlantNameValid()
    }

    private fun isPlantNameValid() = commonNameEditText.isNotEmpty() || scientificNameEditText.isNotEmpty()
}