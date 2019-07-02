package com.geobotanica.geobotanica.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.util.put
import kotlinx.android.synthetic.main.dialog_warning.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class WarningDialog(
        private val titleResId: Int,
        private val messageResId: Int,
        private val sharedPrefsAllowForeverKey: String,
        private val continuation: Continuation<Boolean>
) : DialogFragment() {

    private lateinit var dialog: AlertDialog
    private lateinit var customView: View // Required for kotlinx synthetic bindings

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        customView = LayoutInflater.from(context).inflate(R.layout.dialog_warning, null)

        dialog = with(AlertDialog.Builder(requireContext())) {
            setTitle(getString(titleResId))
            setView(customView)
            setNegativeButton(getString(R.string.no)) { _, _ -> continuation.resume(false) }
            setPositiveButton(getString(R.string.yes)) { _, _ -> continuation.resume(true) }
            create()
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return customView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        text.text = getString(messageResId)
        bindListeners()
    }

    private fun bindListeners() {
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            dialog.getButton(Dialog.BUTTON_NEGATIVE).isEnabled = ! isChecked
            PreferenceManager.getDefaultSharedPreferences(context?.applicationContext)
                    .put(sharedPrefsAllowForeverKey to isChecked)
        }
    }
}