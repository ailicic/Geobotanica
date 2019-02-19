package com.geobotanica.geobotanica.ui.newplantconfirm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.PlantTypeConverter
import com.geobotanica.geobotanica.databinding.FragmentNewPlantConfirmBinding
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.*
import kotlinx.android.synthetic.main.fragment_new_plant_confirm.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*
import kotlinx.android.synthetic.main.measurement_compound_view.view.*
import java.io.File
import javax.inject.Inject

// TODO: Clicking on photo should blow it up (show type icon and edit/delete button there too)

class NewPlantConfirmFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantConfirmViewModel>
    private lateinit var viewModel: NewPlantConfirmViewModel

//    private lateinit var capturingPlantPhotoType: PlantPhoto.Type
//    private var plantPhotoCompoundViews = mutableMapOf<PlantPhoto.Type,PlantPhotoCompoundView>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            plantType = PlantTypeConverter.toPlantType(getFromBundle(plantTypeKey))
            photoUri = getFromBundle(photoUriKey)
            commonName = getNullableFromBundle(commonNameKey)
            latinName = getNullableFromBundle(latinNameKey)
            heightMeasurement = arguments?.getSerializable(heightMeasurementKey) as Measurement?
            diameterMeasurement = arguments?.getSerializable(diameterMeasurementKey) as Measurement?
            trunkDiameterMeasurement = arguments?.getSerializable(trunkDiameterMeasurementKey) as Measurement?
            Lg.d("Fragment args: userId=$userId, plantType=$plantType, commonName=$commonName, " +
                    "latinName=$latinName, photoUri=$photoUri, " +
                    "heightMeasurement=$heightMeasurement, " +
                    "diameterMeasurement=$diameterMeasurement, " +
                    "trunkDiameterMeasurement=$trunkDiameterMeasurement")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentNewPlantConfirmBinding>(
                layoutInflater, R.layout.fragment_new_plant_confirm, container, false).apply {
            viewModel = this@NewPlantConfirmFragment.viewModel
            lifecycleOwner = this@NewPlantConfirmFragment
        }
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setMainPlantPhoto()
        initMeasurementEditViews()
        setGpsLocationFromBundle()
        bindViewModel()
        bindClickListeners()
    }

    private fun setMainPlantPhoto() {
        plantPhotoComplete.doOnPreDraw {
//            viewModel.photoUris[PlantPhoto.Type.COMPLETE]?.let {uri -> plantPhotoComplete.setPhoto(uri)}
        }
    }

    private fun initMeasurementEditViews() {
        heightMeasurementView.textView.text = resources.getString(R.string.height)
        viewModel.heightMeasurement?.let {heightMeasurementView.setMeasurement(it) }

        diameterMeasurementView.textView.text = resources.getString(R.string.diameter)
        viewModel.diameterMeasurement?.let{ diameterMeasurementView.setMeasurement(it) }

        if (viewModel.plantType == Plant.Type.TREE) {
            trunkDiameterMeasurementView.textView.text = resources.getString(R.string.trunk_diameter)
            viewModel.trunkDiameterMeasurement?.let { trunkDiameterMeasurementView.setMeasurement(it) }
        }
    }

//    private val onAddPhoto = Observer<PlantPhoto.Type> { plantPhotoType ->
//        showToast("onAddPhoto")
//        capturingPlantPhotoType = plantPhotoType
//
//        with (viewModel) {
//            val photoFile = createPhotoFile()
//            newPhotoUri = photoFile.absolutePath
//            startPhotoIntent(photoFile)
//        }
//    }

//    private val onEditPhoto = Observer<PlantPhoto.Type> {
    private val onEditPhoto = Observer<Int?> {
//        capturingPlantPhotoType = plantPhotoType

        with (viewModel) {
            val photoFile = createPhotoFile()
            newPhotoUri = photoFile.absolutePath
            startPhotoIntent(photoFile)
        }
    }


//    private val onEditPhotoType = Observer<PlantPhoto.Type> { plantPhotoType ->
//        val dialog = PhotoTypeDialogFragment()
//        dialog.photoTypeSelected.observe(this,Observer<PlantPhoto.Type> {
//
//        })
//        dialog.show(fragmentManager, "tag")
//    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            requestTakePhoto -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Lg.d("New photo received")
                        Lg.d("Deleting old photo: ${viewModel.photoUri}")
                        Lg.d("Delete photo result = ${File(viewModel.photoUri).delete()}")

                        viewModel.photoUri = viewModel.newPhotoUri
                    }
                    Activity.RESULT_CANCELED -> {
                        Lg.d("onActivityResult: RESULT_CANCELED") // "X" in GUI or back button pressed
                    }
                    else -> Lg.d("onActivityResult: Unrecognized code")
                }
            }
        }
    }

    private fun setGpsLocationFromBundle() =
            arguments?.getSerializable(locationKey)?.let { gps.setLocation(it as Location) }

    private fun bindViewModel() {
        plantPhotoComplete.editPhoto.observeAfterUnsubscribe(this, onEditPhoto)
//        plantPhotoComplete.editPhotoType.observeAfterUnsubscribe(this, onEditPhotoType)
    }

    private fun bindClickListeners() {
        editNamesButton.setOnClickListener(::onNamesEditClicked)
//        addPhotoButton.setOnClickListener(::onAddPhotoClicked)
        editMeasurementsButton.setOnClickListener(::onMeasurementsEditClicked)
        fab.setOnClickListener(::onFabClicked)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onNamesEditClicked(view: View) {
        if (!isPlantValid())
            return
        disableTextEdits()
        setNamesEditable(true)
    }

    private fun disableTextEdits() {
        if (commonNameEditText.isVisible || latinNameEditText.isVisible)
            setNamesEditable(false)
        if (heightMeasurementView.isVisible)
            setMeasurementsEditable(false)
    }

    private fun setNamesEditable(enableEdits: Boolean) {
        if (enableEdits) {
            commonNameText.isVisible = false
            commonNameEditText.isVisible =true

            latinNameText.isVisible = false
            latinNameEditText.isVisible = true

            editNamesButton.isVisible = false
            nameDivider.isVisible = false
        } else {
            commonNameEditText.isVisible = false
            if (commonNameEditText.isNotEmpty()) {
                viewModel.commonName = commonNameEditText.editText?.text.toString()
                commonNameText.text = viewModel.commonName
                commonNameText.isVisible = true
            }
            latinNameEditText.isVisible = false
            if (latinNameEditText.isNotEmpty()) {
                viewModel.latinName = latinNameEditText.editText?.text.toString()
                latinNameText.text = viewModel.latinName
                latinNameText.isVisible = true
            }
            editNamesButton.isVisible = true
            nameDivider.isVisible = true
        }
    }

    // TODO: Show icon in top right of image. Show edit icon in bottom right
    // TODO: Create custom view for each image with icons
    // TODO: After add photo, need screen to select photo type, then camera screen to take photo
    // TODO: Use ConstraintLayout for everything
    @Suppress("UNUSED_PARAMETER")
    private fun onAddPhotoClicked(view: View) {
        if (!isPlantValid())
            return
        disableTextEdits()
        showToast("Add photo Clicked")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onMeasurementsEditClicked(view: View) {
        if (!isPlantValid())
            return
        disableTextEdits()
        setMeasurementsEditable(true)
    }

    private fun setMeasurementsEditable(enableEdits: Boolean) {
        if (enableEdits) {
            editMeasurementsButton.isVisible = false
            heightText.isVisible = false
            diameterText.isVisible = false
            trunkDiameterText.isVisible = false
            heightMeasurementView.isVisible = true
            diameterMeasurementView.isVisible = true
            if (viewModel.plantType == Plant.Type.TREE)
                trunkDiameterMeasurementView.isVisible = true
        } else {
            saveMeasurementEdits()
            editMeasurementsButton.isVisible = true
            heightMeasurementView.isVisible = false
            diameterMeasurementView.isVisible = false
            trunkDiameterMeasurementView.isVisible = false
            heightText.isVisible = true
            diameterText.isVisible = true
            if (viewModel.plantType == Plant.Type.TREE) {
                trunkDiameterText.isVisible = true
            }
        }
    }

    private fun saveMeasurementEdits() {
        viewModel.heightMeasurement = heightMeasurementView.getMeasurement()
        heightText.text = viewModel.heightMeasurement?.toHeightString()
        viewModel.diameterMeasurement = diameterMeasurementView.getMeasurement()
        diameterText.text = viewModel.diameterMeasurement?.toDiameterString()
        if (viewModel.plantType == Plant.Type.TREE) {
            viewModel.trunkDiameterMeasurement = trunkDiameterMeasurementView.getMeasurement()
            trunkDiameterText.text = viewModel.trunkDiameterMeasurement?.toTrunkDiameterString()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onFabClicked(view: View) {
        if (!isPlantValid() || !isLocationValid())
            return
        gps.currentLocation?.let { viewModel.location = it }

        viewModel.savePlantComposite()
        showToast("Plant saved") // TODO: Make snackbar

        val navController = activity.findNavController(R.id.fragment)
        navController.popBackStack(R.id.mapFragment, false)
    }

    private fun isPlantValid(): Boolean {
        if (commonNameEditText.isEmpty() && latinNameEditText.isEmpty()) {
            showSnackbar("Provide a plant name")
            return false
        }
        if (viewModel.heightMeasurement != null && isMeasurementEmpty() ) {
            showSnackbar("Provide plant measurements")
            return false
        }
        return true
    }

    private fun isLocationValid(): Boolean {
        if (!gps.gpsSwitch.isEnabled) {
            showSnackbar("Wait for GPS fix")
            return false
        }
        if (!gps.gpsSwitch.isChecked) {
            showSnackbar("Plant position must be held")
            return false
        }
        return true
    }

    private fun isMeasurementEmpty(): Boolean {
        return heightMeasurementView.isEmpty() ||
                diameterMeasurementView.isEmpty() ||
                ( viewModel.plantType == Plant.Type.TREE && trunkDiameterMeasurementView.isEmpty() )
    }
}
