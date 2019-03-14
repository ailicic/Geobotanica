package com.geobotanica.geobotanica.ui.searchplantname

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchFilterOptions
import com.geobotanica.geobotanica.data_taxa.util.defaultFilterFlags
import com.geobotanica.geobotanica.ui.plantNameFilterOptionsKey
import com.geobotanica.geobotanica.util.getValue
import kotlinx.android.synthetic.main.dialog_plant_name_filter.*

class PlantNameFilterOptionsDialog : DialogFragment() {

    private lateinit var customView: View // Required for kotlinx synthetic bindings
    lateinit var onApplyFilters: (filterOptions: SearchFilterOptions) -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        customView = LayoutInflater.from(context).inflate(R.layout.dialog_plant_name_filter, null)
        return activity?.let {
            AlertDialog.Builder(it).run {
                setTitle(getString(R.string.filter))
                setView(customView)
                setPositiveButton(getString(R.string.apply)) { _, _ ->
                    val filterOptions = SearchFilterOptions.fromBooleans(
                            ! checkboxCommon.isChecked,
                            ! checkboxScientific.isChecked,
                            ! checkboxStarred.isChecked,
                            ! checkboxHistory.isChecked
                    )
                    onApplyFilters(filterOptions)
                }
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
        // TODO: Prevent zero (default) from being passed as null below
        val filterOptions = SearchFilterOptions(
                arguments!!.getValue<Int>(plantNameFilterOptionsKey)
                ?: defaultFilterFlags)
        checkboxCommon.isChecked = ! filterOptions.hasFilter(COMMON)
        checkboxScientific.isChecked = ! filterOptions.hasFilter(SCIENTIFIC)
        checkboxStarred.isChecked = ! filterOptions.hasFilter(STARRED)
        checkboxHistory.isChecked = ! filterOptions.hasFilter(USED)

        checkboxScientific.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked)
                dialog.checkboxCommon.isChecked = true
        }
        checkboxCommon.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked)
                dialog.checkboxScientific.isChecked = true
        }
    }
}