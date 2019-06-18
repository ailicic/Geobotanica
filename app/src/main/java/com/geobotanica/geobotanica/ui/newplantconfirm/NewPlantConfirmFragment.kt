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
import com.geobotanica.geobotanica.ui.dialogs.EditMeasurementsDialog
import com.geobotanica.geobotanica.ui.dialogs.EditPlantNameDialog
import com.geobotanica.geobotanica.ui.dialogs.EditPlantTypeDialog
import com.geobotanica.geobotanica.util.*
import kotlinx.android.synthetic.main.fragment_new_plant_confirm.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*
import java.io.File
import javax.inject.Inject


// TODO: Clicking on photo should blow it up (show type icon and edit/delete button there too)

class NewPlantConfirmFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantConfirmViewModel>
    private lateinit var viewModel: NewPlantConfirmViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            photoUri = getFromBundle(photoUriKey)
            plantType.value = Plant.Type.fromFlag(getFromBundle(plantTypeKey))
            commonName.value = getNullableFromBundle(commonNameKey)
            scientificName.value = getNullableFromBundle(scientificNameKey)
            vernacularId = getNullableFromBundle(vernacularIdKey)
            taxonId = getNullableFromBundle(taxonIdKey)
            height.value = getNullableFromBundle(heightMeasurementKey)
            diameter.value = getNullableFromBundle(diameterMeasurementKey)
            trunkDiameter.value = getNullableFromBundle(trunkDiameterMeasurementKey)
            Lg.d("Fragment args: userId=$userId, plantType=$plantType, " +
                    "commonName=$commonName, scientificName=$scientificName, " +
                    "vernId=$vernacularId, taxonId=$taxonId, photoUri=$photoUri, " +
                    "height=$height, diameter=$diameter, trunkDiameter=$trunkDiameter")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentNewPlantConfirmBinding>(
                layoutInflater, R.layout.fragment_new_plant_confirm, container, false).also {
            it.viewModel = viewModel
            it.lifecycleOwner = this
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePlantTypeIcon()
        updateMainPlantPhoto()
        bindClickListeners()
    }

    private fun updatePlantTypeIcon() {
        val plantTypeDrawables = resources.obtainTypedArray(R.array.plantTypes)
        plantTypeButton.setImageResource(plantTypeDrawables.getResourceId(viewModel.plantType.value!!.ordinal, -1))
        plantTypeDrawables.recycle()
    }

    private fun updateMainPlantPhoto() {
        plantPhotoCompoundView.doOnPreDraw { plantPhotoCompoundView.setPhoto(viewModel.photoUri) }
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


//    private val onClickEditPhoto = Observer<Int?> {
//        //        capturingPlantPhotoType = plantPhotoType
//
//        with (viewModel) {
//            val photoFile = createPhotoFile()
//            newPhotoUri = photoFile.absolutePath
//            startPhotoIntent(photoFile)
//        }
//    }

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
        editPlantNameButton.setOnClickListener(::onClickEditNames)
        plantPhotoCompoundView.editPhoto.observeAfterUnsubscribe(this, onClickEditPhoto)
//        addPhotoButton.setOnClickListener(::onAddPhotoClicked) -> Should trigger photo type selection dialog
        editMeasurementsButton.setOnClickListener(::onClickEditMeasurements)
        fab.setOnClickListener(::onFabClicked)
    }


    @Suppress("UNUSED_PARAMETER")
    private fun onClickEditNames(view: View) {
        with(viewModel) {
            EditPlantNameDialog(
                    commonName.value.orEmpty(),
                    scientificName.value.orEmpty(),
                    ::onNewPlantName
            )
        }.show(this.fragmentManager,"tag")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickEditPlantType(view: View) =
        EditPlantTypeDialog(viewModel.plantType.value!!, ::onNewPlantType).show(this.fragmentManager,"tag")

    private fun onNewPlantType(plantType: Plant.Type) {
        viewModel.plantType.value = plantType
        if (plantType != Plant.Type.TREE)
            viewModel.trunkDiameter.value = null
        updatePlantTypeIcon()
    }

    private val onClickEditPhoto = Observer<Int?> {
        val photoFile = createPhotoFile()
        viewModel.newPhotoUri = photoFile.absolutePath
        startPhotoIntent(photoFile)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickEditMeasurements(view: View) {
        with(viewModel) {
            EditMeasurementsDialog(
                    plantType.value!!,
                    height.value,
                    diameter.value,
                    trunkDiameter.value,
                    ::onNewPlantMeasurements
            )
        }.show(this.fragmentManager,"tag")
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
