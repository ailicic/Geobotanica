package com.geobotanica.geobotanica.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.geobotanica.geobotanica.R
import kotlinx.android.synthetic.main.dialog_item_list.*


class ItemListDialog<T: Enum<T>> (
        private val titleResId: Int,
        private val drawableArrayResId: Int,
        private val enumValues: List<T>,
        private val onItemSelected: (T) -> Unit
) : DialogFragment() {

    private lateinit var customView: View // Required for kotlinx synthetic bindings

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        customView = LayoutInflater.from(context).inflate(R.layout.dialog_item_list, null)
        return activity?.let {
            AlertDialog.Builder(it).run {
                setTitle(getString(titleResId))
                setView(customView)
                setNegativeButton(getString(R.string.cancel)) { _, _ -> }
                create()
            }
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return customView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.adapter = ListItemAdapter(
                enumValues,
                drawableArrayResId,
                ::onClickItem)
    }

    private fun onClickItem(selectedItem: T) {
        onItemSelected(selectedItem)
        dialog?.dismiss()
    }
}
