package com.geobotanica.geobotanica.ui.newplantmeasurement

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.PlantTypeConverter
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import com.geobotanica.geobotanica.util.getNullableFromBundle
import com.geobotanica.geobotanica.util.putValue
import kotlinx.android.synthetic.main.fragment_new_plant_measurement.*
import javax.inject.Inject


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
            scientificName = getNullableFromBundle(scientificNameKey)
            vernacularId = getNullableFromBundle(vernacularIdKey)
            taxonId = getNullableFromBundle(taxonIdKey)
            Lg.d("Fragment args: userId=$userId, plantType=$plantType, commonName=$commonName, " +
                    "scientificName=$scientificName, vernId=$vernacularId, taxonId=$taxonId, photoUri=$photoUri")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_plant_measurement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMeasurementCompoundViews()
        bindClickListeners()
    }

    private fun initMeasurementCompoundViews() {
        if (viewModel.plantType != Plant.Type.TREE)
            trunkDiameterMeasurementView.isVisible = false
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
            heightMeasurementView.isVisible = true
            diameterMeasurementView.isVisible = true
            if (viewModel.plantType == Plant.Type.TREE)
                trunkDiameterMeasurementView.isVisible = true
        } else {
            manualRadioButton.isEnabled = false
            assistedRadioButton.isEnabled = false
            heightMeasurementView.isVisible = false
            diameterMeasurementView.isVisible = false
            trunkDiameterMeasurementView.isVisible = false
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onRadioButtonChecked(radioGroup: RadioGroup, checkedId: Int) {
        when (checkedId) {
            manualRadioButton.id -> {
                Lg.d("onRadioButtonChecked(): Manual")
                heightMeasurementView.isVisible = true
                diameterMeasurementView.isVisible = true

                if (viewModel.plantType == Plant.Type.TREE)
                    trunkDiameterMeasurementView.isVisible = true
            }
            assistedRadioButton.id -> {
                Lg.d("onRadioButtonChecked(): Assisted")
                heightMeasurementView.isVisible = false
                diameterMeasurementView.isVisible = false
                trunkDiameterMeasurementView.isVisible = false
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onFabPressed(view: View) {
        if (!isPlantValid())
            return
        saveViewModelState()

        val navController = activity.findNavController(R.id.fragment)
        navController.navigate(R.id.newPlantConfirmFragment, createBundle())
    }

    private fun saveViewModelState() {
        if (measurementsSwitch.isChecked) {
            viewModel.heightMeasurement = heightMeasurementView.getMeasurement()
            viewModel.diameterMeasurement = diameterMeasurementView.getMeasurement()
            if (viewModel.plantType == Plant.Type.TREE)
                viewModel.trunkDiameterMeasurement = trunkDiameterMeasurementView.getMeasurement()
        }
    }

    private fun isPlantValid(): Boolean {
        if (measurementsSwitch.isChecked && isMeasurementEmpty() ) {
            showSnackbar("Provide plant measurements")
            return false
        }
        return true
    }

    private fun isMeasurementEmpty(): Boolean {
        return heightMeasurementView.isEmpty() ||
                diameterMeasurementView.isEmpty() ||
                ( viewModel.plantType == Plant.Type.TREE && trunkDiameterMeasurementView.isEmpty() )
    }

    private fun createBundle(): Bundle {
        return bundleOf(
                userIdKey to viewModel.userId,
                plantTypeKey to viewModel.plantType.ordinal,
                photoUriKey to viewModel.photoUri
        ).apply {
            viewModel.commonName?.let { putValue(commonNameKey, it) }
            viewModel.scientificName?.let { putValue(scientificNameKey, it) }
            viewModel.vernacularId?.let { putValue(vernacularIdKey, it) }
            viewModel.taxonId?.let { putValue(taxonIdKey, it) }
            viewModel.heightMeasurement?.let { putSerializable(heightMeasurementKey, it) }
            viewModel.diameterMeasurement?.let { putSerializable(diameterMeasurementKey, it) }
            viewModel.trunkDiameterMeasurement?.let { putSerializable(trunkDiameterMeasurementKey, it) }
        }
    }
}
