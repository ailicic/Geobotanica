package com.geobotanica.geobotanica.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.geobotanica.geobotanica.R

class WarningDialog(
        private val title: String,
        private val message: String,
        private val onClickYes: () -> Unit
) : DialogFragment() {

    private lateinit var dialog: AlertDialog
    private lateinit var customView: View // Required for kotlinx synthetic bindings

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        customView = LayoutInflater.from(context).inflate(R.layout.dialog_warning, null)

        dialog = with(AlertDialog.Builder(requireContext())) {
            setTitle(title)
            setMessage(message)
            setView(customView)
            setNegativeButton(getString(R.string.no)) { _, _ -> }
            setPositiveButton(getString(R.string.yes)) { _, _ -> onClickYes() }
            create()
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return customView
    }
}