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
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.Plant.Type.TREE
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import com.geobotanica.geobotanica.databinding.FragmentNewPlantConfirmBinding
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.dialog.EditPlantNameDialog
import com.geobotanica.geobotanica.ui.dialog.InputMeasurementsDialog
import com.geobotanica.geobotanica.ui.viewpager.PlantPhotoAdapter
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import com.geobotanica.geobotanica.util.getNullableFromBundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.compound_gps.view.*
import kotlinx.android.synthetic.main.fragment_new_plant_confirm.*
import kotlinx.coroutines.launch
import javax.inject.Inject


// TODO: Use newer version of ListAdapter with auto-diffing (or create new SingleLiveEvent for deleting photo smooth scroll)

@Suppress("UNUSED_PARAMETER")
class NewPlantConfirmFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantConfirmViewModel>
    private lateinit var viewModel: NewPlantConfirmViewModel

    private lateinit var photoAdapter: PlantPhotoAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            init(
                getFromBundle(userIdKey),
                getFromBundle(photoUriKey),
                getNullableFromBundle(commonNameKey),
                getNullableFromBundle(scientificNameKey),
                getNullableFromBundle(vernacularIdKey),
                getNullableFromBundle(taxonIdKey),
                Plant.Type.fromFlag(getFromBundle(plantTypeKey)),
                getNullableFromBundle(heightMeasurementKey),
                getNullableFromBundle(diameterMeasurementKey),
                getNullableFromBundle(trunkDiameterMeasurementKey)
            )
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
            initUi()
            bindViewModel()
            bindListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.toolbar.setNavigationOnClickListener(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == requestTakePhoto) {
            when (resultCode) {
                Activity.RESULT_OK -> viewModel.onPhotoComplete(plantPhotoPager.currentItem)
                Activity.RESULT_CANCELED -> viewModel.deleteTemporaryPhoto()  // "X" in GUI or back button pressed
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
                viewModel.deleteAllPhotos()
                activity.currentLocation = null
                showToast(getString(R.string.plant_discarded))
                popUpTo(R.id.mapFragment)
            }
            setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
            create()
        }.show()
    }

    private fun bindViewModel() {
        with(viewModel) {
            photoData.observe(viewLifecycleOwner, Observer { photoAdapter.items = it; photoAdapter.notifyDataSetChanged() } )
            showPhotoDeletedToast.observe(viewLifecycleOwner, Observer { showToast(getString(R.string.photo_deleted)) })
            startPhotoIntent.observe(viewLifecycleOwner, Observer { startPhotoIntent(it) })
            showAddedPhoto.observe(viewLifecycleOwner, Observer { plantPhotoPager.setCurrentItem(photoAdapter.items.size - 1, true) })
        }
    }

    private fun initUi() {
        fun onClickPhoto() { /* TODO: Clicking on the photo should blow it up */ }
        fun onClickDeletePhoto() = viewModel.deletePhoto(plantPhotoPager.currentItem)
        fun onClickRetakePhoto() = viewModel.retakePhoto()
        fun onClickAddPhoto(photoType: PlantPhoto.Type) = viewModel.addPhoto(photoType)

        viewModel.plantType.value?.let { plantTypeButton.initIcon(it) }
        photoAdapter = PlantPhotoAdapter(
                ::onClickPhoto,
                ::onClickDeletePhoto,
                ::onClickRetakePhoto,
                ::onClickAddPhoto,
                viewModel.plantType,
                this)
        plantPhotoPager.adapter = photoAdapter
    }

    private fun bindListeners() {
        plantTypeButton.onNewPlantType = { viewModel.onNewPlantType(it) }
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
                    plantType.value ?: TREE,
                    height.value,
                    diameter.value,
                    trunkDiameter.value,
                    ::onMeasurementsUpdated
            ).show(requireFragmentManager(), "tag")
        }
    }

    private fun onFabClicked(view: View) {
        lifecycleScope.launch {
            if (!verifyPlantLocation())
                return@launch
            gps.currentLocation?.let { viewModel.savePlantComposite(it) }

            showToast("Plant saved")
            findNavController().popBackStack(R.id.mapFragment, false)
            activity.currentLocation = null
        }
    }

    private fun verifyPlantLocation(): Boolean {
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