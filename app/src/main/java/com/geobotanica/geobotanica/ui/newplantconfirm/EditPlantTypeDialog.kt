package com.geobotanica.geobotanica.ui.newplantconfirm

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.ui.newplanttype.PlantTypeAdapter
import kotlinx.android.synthetic.main.dialog_plant_type.*

class EditPlantTypeDialog : DialogFragment() {

    private lateinit var customView: View // Required for kotlinx synthetic bindings
    lateinit var onPlantTypeSelected: (plantType: Plant.Type) -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        customView = LayoutInflater.from(context).inflate(R.layout.dialog_plant_type, null)
        return activity?.let {
            AlertDialog.Builder(it).run {
                setTitle(getString(R.string.select_type))
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
        recyclerView.adapter = PlantTypeAdapter(Plant.Type.values().toList(), ::onPlantTypeClicked)
    }

    private fun onPlantTypeClicked(plantType: Plant.Type) {
        onPlantTypeSelected(plantType)
        dialog.dismiss()
    }
}