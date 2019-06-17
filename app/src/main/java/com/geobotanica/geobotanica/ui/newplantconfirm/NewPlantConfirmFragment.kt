package com.geobotanica.geobotanica.ui.newplantconfirm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.databinding.FragmentNewPlantConfirmBinding
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import com.geobotanica.geobotanica.util.getNullableFromBundle
import com.geobotanica.geobotanica.util.observeAfterUnsubscribe
import kotlinx.android.synthetic.main.fragment_new_plant_confirm.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*
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
            photoUri = getFromBundle(photoUriKey)
            commonName = getNullableFromBundle(commonNameKey)
            scientificName = getNullableFromBundle(scientificNameKey)
            vernacularId = getNullableFromBundle(vernacularIdKey)
            taxonId = getNullableFromBundle(taxonIdKey)
            plantType = Plant.Type.fromFlag(getFromBundle(plantTypeKey))
            heightMeasurement = getNullableFromBundle(heightMeasurementKey)
            diameterMeasurement = getNullableFromBundle(diameterMeasurementKey)
            trunkDiameterMeasurement = getNullableFromBundle(trunkDiameterMeasurementKey)
            Lg.d("Fragment args: userId=$userId, plantType=$plantType, " +
                    "commonName=$commonName, scientificName=$scientificName, " +
                    "vernId=$vernacularId, taxonId=$taxonId, photoUri=$photoUri, " +
                    "height=$heightMeasurement, diameter=$diameterMeasurement, trunkDiameter=$trunkDiameterMeasurement")
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
        bindViewModel()
        bindClickListeners()
    }

    private fun setMainPlantPhoto() {
        plantPhotoFull.doOnPreDraw { plantPhotoFull.setPhoto(viewModel.photoUri) }
    }

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
                    Activity.RESULT_CANCELED -> { // "X" in GUI or back button pressed
                        Lg.d("onActivityResult: RESULT_CANCELED")
                        Lg.d("Deleting unused photo file: ${viewModel.newPhotoUri}")
                        Lg.d("Delete photo result = ${File(viewModel.newPhotoUri).delete()}")
                    }
                    else -> Lg.d("onActivityResult: Unrecognized code")
                }
            }
            else -> showToast("Unrecognized request code")
        }
    }

    private fun bindViewModel() {
        plantPhotoFull.editPhoto.observeAfterUnsubscribe(this, onClickEditPhoto)
//        plantPhotoComplete.editPhotoType.observeAfterUnsubscribe(this, onEditPhotoType) -> TRIGGERED BY EDIT PHOTO DIALOG
    }

    private val onClickEditPhoto = Observer<Int?> {
        //        capturingPlantPhotoType = plantPhotoType

        with (viewModel) {
            val photoFile = createPhotoFile()
            newPhotoUri = photoFile.absolutePath
            startPhotoIntent(photoFile)
        }
    }

//    private val onAddPhoto = Observer<PlantPhoto.Type> { plantPhotoType ->
//        capturingPlantPhotoType = plantPhotoType
//
//        with (viewModel) {
//            val photoFile = createPhotoFile()
//            newPhotoUri = photoFile.absolutePath
//            startPhotoIntent(photoFile)
//        }
//    }

//    private val onEditPhotoType = Observer<PlantPhoto.Type> { plantPhotoType ->
//        val dialog = PhotoTypeDialogFragment()
//        dialog.photoTypeSelected.observe(this,Observer<PlantPhoto.Type> {
//
//        })
//        dialog.show(fragmentManager, "tag")
//    }

    private fun bindClickListeners() {
        plantTypeButton.setOnClickListener(::onClickEditPlantType)
//        editNamesButton.setOnClickListener(::onClickEditNames) -> NEED DIALOG
//        addPhotoButton.setOnClickListener(::onAddPhotoClicked) -> Should trigger photo type selection dialog
//        editMeasurementsButton.setOnClickListener(::onMeasurementsEditClicked) -> NEED DIALOG
        fab.setOnClickListener(::onFabClicked)
    }


    @Suppress("UNUSED_PARAMETER")
    private fun onClickEditPlantType(view: View) {
        EditPlantTypeDialog().run {
            onPlantTypeSelected = ::onNewPlantType
            show(this@NewPlantConfirmFragment.fragmentManager,"tag")
        }
    }

    private fun onNewPlantType(plantType: Plant.Type) {
        val plantTypeDrawables = resources.obtainTypedArray(R.array.plantTypes)
        plantTypeButton.setImageResource(plantTypeDrawables.getResourceId(plantType.ordinal, -1))
        plantTypeDrawables.recycle()
        viewModel.plantType = plantType
    }



    // TODO: After add photo, need screen to select photo type, then camera screen to take photo
    // TODO: Use ConstraintLayout for everything
//    @Suppress("UNUSED_PARAMETER")
//    private fun onAddPhotoClicked(view: View) {
//        if (!isPlantNameValid())
//            return
//        disableTextEdits()
//        showToast("Add photo Clicked")
//    }


    @Suppress("UNUSED_PARAMETER")
    private fun onFabClicked(view: View) {
        if (!isLocationValid())
            return
        gps.currentLocation?.let { viewModel.location = it }

        viewModel.savePlantComposite()
        showToast("Plant saved") // TODO: Make snackbar (maybe?)

        val navController = activity.findNavController(R.id.fragment)
        navController.popBackStack(R.id.mapFragment, false)
        activity.currentLocation = null
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
}


///////////////// MOVE TO EDIT PLANT NAME DIALOG

//        commonNameEditText.onTextChanged(::onCommonEditTextChanged)
//        resetCommonButton.setOnClickListener(::onClickResetCommon)
//        scientificNameEditText.onTextChanged(::onScientificEditTextChanged)
//        resetScientificButton.setOnClickListener(::onResetScientificName)


//    private fun onCommonEditTextChanged(editText: String) {
//        resetCommonButton.isVisible = editText != viewModel.commonName
//    }
//
//    @Suppress("UNUSED_PARAMETER")
//    private fun onClickResetCommon(view: View) {
//        commonNameEditText.setText(viewModel.commonName)
//        commonNameEditText.setSelection(viewModel.commonName!!.length)
//    }

//    private fun onScientificEditTextChanged(editText: String) {
//        resetScientificButton.isVisible = editText != viewModel.scientificName
//    }
//
//    @Suppress("UNUSED_PARAMETER")
//    private fun onResetScientificName(view: View) {
//        scientificNameEditText.setText(viewModel.scientificName)
//        scientificNameEditText.setSelection(viewModel.scientificName!!.length)
//    }

//    @Suppress("UNUSED_PARAMETER")
//    private fun onClickEditNames(view: View) {
//        if (!isPlantNameValid())
//            return
//        disableTextEdits()
//        setNamesEditable(true)
//    }

//    private fun updateViewModel() {
//        val commonName = commonNameTextInput.toTrimmedString()
//        val scientificName = scientificNameTextInput.toTrimmedString()

//        viewModel.commonName = if (commonName.isEmpty()) null else commonName
//        viewModel.scientificName = if (scientificName.isEmpty()) null else scientificName
//    }

//    private fun nullIdsIfInvalid() {
//        viewModel.vernacularId?.let {
//            if (commonNameTextInput.toTrimmedString() != viewModel.commonName)
//                viewModel.vernacularId = null
//        }
//        viewModel.taxonId?.let {
//            if (scientificNameTextInput.toTrimmedString() != viewModel.scientificName)
//                viewModel.taxonId = null
//        }
//    }

//    private fun isPlantNameValid(): Boolean {
//        if (commonNameTextInput.isEmpty() && scientificNameTextInput.isEmpty()) {
//            showSnackbar("Provide a plant name")
//            return false
//        }

//        if (viewModel.heightMeasurement != null && isMeasurementEmpty() ) {
//            showSnackbar("Provide plant measurements")
//            return false
//        }
//        return true
//    }


///////////////// MOVE TO EDIT MEASUREMENTS DIALOG

//    private fun initMeasurementEditViews() {
//        viewModel.heightMeasurement?.let { heightMeasurementView.setMeasurement(it) }
//        viewModel.diameterMeasurement?.let{ diameterMeasurementView.setMeasurement(it) }
//
//        if (viewModel.plantType == Plant.Type.TREE) {
//            viewModel.trunkDiameterMeasurement?.let { trunkDiameterMeasurementView.setMeasurement(it) }
//        }
//    }

//    @Suppress("UNUSED_PARAMETER")
//    private fun onMeasurementsEditClicked(view: View) {
//        if (!isPlantNameValid())
//            return
//        disableTextEdits()
//        setMeasurementsEditable(true)
//    }


//    private fun saveMeasurementEdits() {
//        viewModel.heightMeasurement = heightMeasurementView.getMeasurement()
//        heightText.text = viewModel.heightMeasurement?.toHeightString()
//        viewModel.diameterMeasurement = diameterMeasurementView.getMeasurement()
//        diameterText.text = viewModel.diameterMeasurement?.toDiameterString()
//        if (viewModel.plantType == Plant.Type.TREE) {
//            viewModel.trunkDiameterMeasurement = trunkDiameterMeasurementView.getMeasurement()
//            trunkDiameterText.text = viewModel.trunkDiameterMeasurement?.toTrunkDiameterString()
//        }
//    }

//    private fun isMeasurementEmpty(): Boolean {
//        return heightMeasurementView.isEmpty() ||
//                diameterMeasurementView.isEmpty() ||
//                ( viewModel.plantType == Plant.Type.TREE && trunkDiameterMeasurementView.isEmpty() )
//    }