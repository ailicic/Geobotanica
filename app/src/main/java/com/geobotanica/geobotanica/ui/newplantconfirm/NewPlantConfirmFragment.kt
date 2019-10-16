package com.geobotanica.geobotanica.ui.newplantconfirm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import com.geobotanica.geobotanica.databinding.FragmentNewPlantConfirmBinding
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.dialog.EditPlantNameDialog
import com.geobotanica.geobotanica.ui.dialog.InputMeasurementsDialog
import com.geobotanica.geobotanica.ui.viewpager.PhotoData
import com.geobotanica.geobotanica.ui.viewpager.PlantPhotoAdapter
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import com.geobotanica.geobotanica.util.getNullableFromBundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.compound_gps.view.*
import kotlinx.android.synthetic.main.fragment_new_plant_confirm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


@Suppress("UNUSED_PARAMETER")
class NewPlantConfirmFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantConfirmViewModel>
    private lateinit var viewModel: NewPlantConfirmViewModel

    @Inject lateinit var storageHelper: StorageHelper

    private lateinit var photoAdapter: PlantPhotoAdapter

    private var isPhotoRetake: Boolean = false
    private lateinit var newPhotoType: PlantPhoto.Type
    private lateinit var newPhotoUri: String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            photos.clear()
            photos.add(PhotoData(PlantPhoto.Type.COMPLETE, getFromBundle(photoUriKey)))
            plantType.value = Plant.Type.fromFlag(getFromBundle(plantTypeKey))
            commonName.value = getNullableFromBundle(commonNameKey)
            scientificName.value = getNullableFromBundle(scientificNameKey)
            vernacularId = getNullableFromBundle(vernacularIdKey)
            taxonId = getNullableFromBundle(taxonIdKey)
            height.value = getNullableFromBundle(heightMeasurementKey)
            diameter.value = getNullableFromBundle(diameterMeasurementKey)
            trunkDiameter.value = getNullableFromBundle(trunkDiameterMeasurementKey)
            Lg.d("Fragment args: userId=$userId, photoType=${plantType.value}, " +
                    "commonName=${commonName.value}, scientificName=${scientificName.value}, " +
                    "vernId=$vernacularId, taxonId=$taxonId, photos=$photos, " +
                    "height=${height.value}, diameter=${diameter.value}, trunkDiameter=${trunkDiameter.value}")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentNewPlantConfirmBinding>(
                layoutInflater, R.layout.fragment_new_plant_confirm, container, false).also {
            it.viewModel = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addOnBackPressedCallback()
        initPlantTypeButton()
        initPhotoViewPager()
        bindClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.toolbar.setNavigationOnClickListener(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == requestTakePhoto) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val photoIndex = plantPhotoPager.currentItem
                    Lg.d("onActivityResult: RESULT_OK (New photo received)")
                    if (isPhotoRetake) {
                        val oldPhotoUri = viewModel.photos[photoIndex].photoUri
                        Lg.d("Deleting old photo: $oldPhotoUri (Result = ${File(oldPhotoUri).delete()})")
                        viewModel.photos[photoIndex].photoUri = newPhotoUri
                        photoAdapter.notifyDataSetChanged()
                    } else {
                        viewModel.photos.add(PhotoData(newPhotoType, newPhotoUri))
                        photoAdapter.notifyDataSetChanged()
                        lifecycleScope.launch(Dispatchers.Main) {
                            delay(300)
                            plantPhotoPager.setCurrentItem(viewModel.photos.size, true)
                        }
                    }
                }
                Activity.RESULT_CANCELED -> { // "X" in GUI or back button pressed
                    Lg.d("onActivityResult: RESULT_CANCELED")
                    Lg.d("Deleting unused photo file: $newPhotoUri (Result = ${File(newPhotoUri).delete()})")
                }
                else -> Lg.d("onActivityResult: Unrecognized code")
            }
        }
    }

    private fun addOnBackPressedCallback() {
        activity.toolbar.setNavigationOnClickListener { onClickBackButton() }
        requireActivity().onBackPressedDispatcher.addCallback(this) { onClickBackButton() }
    }

    private fun onClickBackButton() {
        AlertDialog.Builder(activity).apply {
            setTitle(getString(R.string.discard_new_plant))
            setMessage(getString(R.string.discard_new_plant_confirm))
            setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.photos.forEach {
                    val photoUri = it.photoUri
                    Lg.d("Deleting old photo (result = ${File(photoUri).delete()}): $photoUri")
                }
                activity.currentLocation = null
                showToast(getString(R.string.plant_discarded))
                popUpTo(R.id.mapFragment)
            }
            setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
            create()
        }.show()
    }

    private fun initPlantTypeButton() {
        plantTypeButton.init(viewModel.plantType.value!!)
        plantTypeButton.onNewPlantType = {
            viewModel.plantType.value = it
        }
    }

    private fun initPhotoViewPager() {
        photoAdapter = PlantPhotoAdapter(
                ::onClickPhoto,
                ::onDeletePhoto,
                ::onRetakePhoto,
                ::onAddPhoto,
                viewModel.plantType,
                this)
        photoAdapter.items = viewModel.photos
        plantPhotoPager.adapter = photoAdapter
    }

    private fun onClickPhoto() {
        // TODO: Clicking on the photo should blow it up
    }

    private fun onDeletePhoto() {
        val photoIndex = plantPhotoPager.currentItem
        val photoUri = viewModel.photos[photoIndex].photoUri
        Lg.d("Deleting old photo: $photoUri (Result = ${File(photoUri).delete()})")
        lifecycleScope.launch(Dispatchers.Main) {
            delay(300)
            viewModel.photos.removeAt(photoIndex)
            photoAdapter.notifyItemRemoved(photoIndex)
            showToast(getString(R.string.photo_deleted))
        }
    }

    private fun onRetakePhoto() {
        isPhotoRetake = true
        val photoFile = storageHelper.createPhotoFile()
        newPhotoUri = photoFile.absolutePath
        startPhotoIntent(photoFile)
    }

    private fun onAddPhoto(photoType: PlantPhoto.Type) {
        isPhotoRetake = false
        newPhotoType = photoType
        val photoFile = storageHelper.createPhotoFile()
        newPhotoUri = photoFile.absolutePath
        startPhotoIntent(photoFile)
    }

    private fun bindClickListeners() {
        editPlantNameButton.setOnClickListener(::onClickEditNames)
        editMeasurementsButton.setOnClickListener(::onClickEditMeasurements)
        fab.setOnClickListener(::onFabClicked)
    }

    private fun onClickEditNames(view: View) {
        with(viewModel) {
            EditPlantNameDialog(
                    commonName.value.orEmpty(),
                    scientificName.value.orEmpty(),
                    ::onNewPlantName
            )
        }.show(requireFragmentManager(),"tag")
    }

    private fun onClickEditMeasurements(view: View) {
        with(viewModel) {
            InputMeasurementsDialog(
                    R.string.edit_plant_measurements,
                    plantType.value!!,
                    height.value,
                    diameter.value,
                    trunkDiameter.value,
                    ::onMeasurementsUpdated
            ).show(requireFragmentManager(), "tag")
        }
    }

    private fun onFabClicked(view: View) {
        lifecycleScope.launch {
            if (!isLocationValid())
                return@launch
            gps.currentLocation?.let { viewModel.location = it }

            viewModel.savePlantComposite()
            showToast("Plant saved") // TODO: Make snackbar (maybe?)

            val navController = NavHostFragment.findNavController(this@NewPlantConfirmFragment)
            navController.popBackStack(R.id.mapFragment, false)
            activity.currentLocation = null
        }
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
