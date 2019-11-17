package com.geobotanica.geobotanica.ui.searchplantname

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data_taxa.entity.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchFilterOptions
import com.geobotanica.geobotanica.data_taxa.util.defaultFilterFlags
import com.geobotanica.geobotanica.ui.plantNameFilterOptionsKey
import com.geobotanica.geobotanica.util.getValue
import kotlinx.android.synthetic.main.dialog_plant_name_filter.*

class PlantNameFilterOptionsDialog : DialogFragment() {

    private lateinit var customView: View // Required for kotlinx synthetic bindings
    lateinit var onApplyFilters: (filterOptions: SearchFilterOptions) -> Unit

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        customView = LayoutInflater.from(context).inflate(R.layout.dialog_plant_name_filter, null)
        return activity?.let {
            AlertDialog.Builder(it).run {
                setTitle(getString(R.string.include))
                setView(customView)
                setPositiveButton(getString(R.string.apply)) { _, _ ->
                    val filterOptions = SearchFilterOptions.fromBooleans(
                            ! commonCheckbox.isChecked,
                            ! scientificCheckbox.isChecked,
                            ! starredCheckbox.isChecked,
                            ! historyCheckbox.isChecked
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
        val filterOptions = SearchFilterOptions(
                arguments?.getValue<Int>(plantNameFilterOptionsKey)
                ?: defaultFilterFlags
        )
        commonCheckbox.isChecked = ! filterOptions.hasFilter(COMMON)
        scientificCheckbox.isChecked = ! filterOptions.hasFilter(SCIENTIFIC)
        starredCheckbox.isChecked = ! filterOptions.hasFilter(STARRED)
        historyCheckbox.isChecked = ! filterOptions.hasFilter(USED)

        scientificCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked)
                dialog!!.commonCheckbox.isChecked = true
        }
        commonCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked)
                dialog!!.scientificCheckbox.isChecked = true
        }
    }
}