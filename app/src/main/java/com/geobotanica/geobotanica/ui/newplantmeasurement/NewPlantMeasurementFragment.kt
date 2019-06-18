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
            photoUri = getFromBundle(photoUriKey)
            commonName = getNullableFromBundle(commonNameKey)
            scientificName = getNullableFromBundle(scientificNameKey)
            vernacularId = getNullableFromBundle(vernacularIdKey)
            taxonId = getNullableFromBundle(taxonIdKey)
            plantType = Plant.Type.fromFlag(getFromBundle(plantTypeKey))
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
            trunkDiameterEditView.isVisible = false
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
            heightEditView.isVisible = true
            diameterEditView.isVisible = true
            if (viewModel.plantType == Plant.Type.TREE)
                trunkDiameterEditView.isVisible = true
        } else {
            manualRadioButton.isEnabled = false
            assistedRadioButton.isEnabled = false
            heightEditView.isVisible = false
            diameterEditView.isVisible = false
            trunkDiameterEditView.isVisible = false
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onRadioButtonChecked(radioGroup: RadioGroup, checkedId: Int) {
        when (checkedId) {
            manualRadioButton.id -> {
                Lg.d("onRadioButtonChecked(): Manual")
                heightEditView.isVisible = true
                diameterEditView.isVisible = true

                if (viewModel.plantType == Plant.Type.TREE)
                    trunkDiameterEditView.isVisible = true
            }
            assistedRadioButton.id -> {
                Lg.d("onRadioButtonChecked(): Assisted")
                heightEditView.isVisible = false
                diameterEditView.isVisible = false
                trunkDiameterEditView.isVisible = false
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onFabPressed(view: View) {
        if (!isPlantValid()) {
            showSnackbar(resources.getString(R.string.provide_plant_measurements))
            return
        }
        saveViewModelState()

        val navController = activity.findNavController(R.id.fragment)
        navController.navigate(R.id.newPlantConfirmFragment, createBundle())
    }

    private fun saveViewModelState() {
        if (measurementsSwitch.isChecked) {
            viewModel.heightMeasurement = heightEditView.measurement
            viewModel.diameterMeasurement = diameterEditView.measurement
            if (viewModel.plantType == Plant.Type.TREE)
                viewModel.trunkDiameterMeasurement = trunkDiameterEditView.measurement
        }
    }

    private fun isPlantValid(): Boolean = ! measurementsSwitch.isChecked || isMeasurementValid()


    private fun isMeasurementValid(): Boolean {
        return heightEditView.isNotEmpty() || diameterEditView.isNotEmpty() ||
                ( viewModel.plantType == Plant.Type.TREE && trunkDiameterEditView.isNotEmpty() )
    }

    private fun createBundle(): Bundle {
        return bundleOf(
                userIdKey to viewModel.userId,
                photoUriKey to viewModel.photoUri,
                plantTypeKey to viewModel.plantType.flag
        ).apply {
            viewModel.commonName?.let { putValue(commonNameKey, it) }
            viewModel.scientificName?.let { putValue(scientificNameKey, it) }
            viewModel.vernacularId?.let { putValue(vernacularIdKey, it) }
            viewModel.taxonId?.let { putValue(taxonIdKey, it) }
            if (measurementsSwitch.isChecked) {
                viewModel.heightMeasurement?.let { putSerializable(heightMeasurementKey, it) }
                viewModel.diameterMeasurement?.let { putSerializable(diameterMeasurementKey, it) }
                viewModel.trunkDiameterMeasurement?.let { putSerializable(trunkDiameterMeasurementKey, it) }
            }
        }
    }
}
