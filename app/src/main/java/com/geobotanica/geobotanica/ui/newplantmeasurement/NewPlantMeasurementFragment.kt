package com.geobotanica.geobotanica.ui.newplantmeasurement

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.PlantTypeConverter
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.NavBundleExt.getFromBundle
import com.geobotanica.geobotanica.util.NavBundleExt.getNullableFromBundle
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_new_plant_measurement.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*
import kotlinx.android.synthetic.main.measurement_compound_view.view.*
import javax.inject.Inject


// TODO: Handle back button better. Delete temp photo if needed, allow edit prev. activity, etc.

class NewPlantMeasurementFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantMeasurementViewModel>
    private lateinit var viewModel: NewPlantMeasurementViewModel


    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            plantType = PlantTypeConverter.toPlantType(getFromBundle(plantTypeKey))
            photoUri = getFromBundle(photoUriKey)
            commonName = getNullableFromBundle(commonNameKey)
            latinName = getNullableFromBundle(latinNameKey)
            Lg.d("Fragment args: userId=$userId, plantType=$plantType, commonName=$commonName, " +
                    "latinName=$latinName, photoUri=$photoUri")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_plant_measurement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setGpsLocationFromBundle()
        initMeasurementCompoundViews()
        bindClickListeners()
    }

    private fun setGpsLocationFromBundle() =
        arguments?.getSerializable(locationKey)?.let { gps.setLocation(it as Location) }

    private fun initMeasurementCompoundViews() {
        heightMeasurement.textView.text = resources.getString(R.string.height)
        diameterMeasurement.textView.text = resources.getString(R.string.diameter)
        trunkDiameterMeasurement.textView.text = resources.getString(R.string.trunk_diameter)
        if (viewModel.plantType != Plant.Type.TREE)
            trunkDiameterMeasurement.visibility = View.GONE
    }

    private fun bindClickListeners() {
        measurementsSwitch.setOnCheckedChangeListener(::onToggleAddMeasurement)
        measurementRadioGroup.setOnCheckedChangeListener(::onRadioButtonChecked)
        fab.setOnClickListener(::onFabPressed)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onToggleAddMeasurement(buttonView: CompoundButton, isChecked: Boolean) {
        Lg.d("onToggleHoldPosition(): isChecked=$isChecked")
        if (isChecked) {
            manualRadioButton.isEnabled = true
            assistedRadioButton.isEnabled = true
            heightMeasurement.visibility = View.VISIBLE
            diameterMeasurement.visibility = View.VISIBLE
            if (viewModel.plantType == Plant.Type.TREE)
                trunkDiameterMeasurement.visibility = View.VISIBLE
        } else {
            manualRadioButton.isEnabled = false
            assistedRadioButton.isEnabled = false
            heightMeasurement.visibility = View.GONE
            diameterMeasurement.visibility = View.GONE
            trunkDiameterMeasurement.visibility = View.GONE
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onRadioButtonChecked(radioGroup: RadioGroup, checkedId: Int) {
        when (checkedId) {
            manualRadioButton.id -> {
                Lg.d("onRadioButtonChecked(): Manual")
                heightMeasurement.visibility = View.VISIBLE
                diameterMeasurement.visibility = View.VISIBLE

                if (viewModel.plantType == Plant.Type.TREE)
                    trunkDiameterMeasurement.visibility = View.VISIBLE
            }
            assistedRadioButton.id -> {
                Lg.d("onRadioButtonChecked(): Assisted")
                heightMeasurement.visibility = View.GONE
                diameterMeasurement.visibility = View.GONE
                trunkDiameterMeasurement.visibility = View.GONE
            }
        }
    }

    private fun onFabPressed(view: View) {
        if (!isPlantValid(view))
            return
        loadViewModelWithPlantData()
        viewModel.savePlantComposite()

        Toast.makeText(activity, "Plant saved", Toast.LENGTH_SHORT).show() // TODO: Make snackbar

        val navController = activity.findNavController(R.id.fragment)
        navController.popBackStack(R.id.mapFragment, false)
    }

    private fun isPlantValid(view: View): Boolean {
        if (!gps.gpsSwitch.isEnabled) {
            Snackbar.make(view, "Wait for GPS fix", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return false
        }
        if (!gps.gpsSwitch.isChecked) {
            Snackbar.make(view, "Plant position must be held", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return false
        }
        if (measurementsSwitch.isChecked && isMeasurementEmpty() ) {
            Snackbar.make(view, "Provide plant plantMeasurements", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return false
        }
        return true
    }

    private fun isMeasurementEmpty(): Boolean {
        return heightMeasurement.editText.text.isEmpty() ||
                diameterMeasurement.editText.text.isEmpty() ||
                ( viewModel.plantType == Plant.Type.TREE && trunkDiameterMeasurement.editText.text.isEmpty() )
    }

    private fun loadViewModelWithPlantData() {
        if (measurementsSwitch.isChecked) {
            viewModel.height = heightMeasurement.getInCentimeters()
            viewModel.diameter = diameterMeasurement.getInCentimeters()
            if (viewModel.plantType == Plant.Type.TREE)
                viewModel.trunkDiameter = trunkDiameterMeasurement.getInCentimeters()
        }

        gps.currentLocation?.let { viewModel.location = it }
    }
}
