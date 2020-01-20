package com.geobotanica.geobotanica.ui.plantdetail

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import com.geobotanica.geobotanica.databinding.FragmentPlantDetailBinding
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.dialog.EditPlantNameDialog
import com.geobotanica.geobotanica.ui.dialog.InputMeasurementsDialog
import com.geobotanica.geobotanica.ui.viewpager.PlantPhotoAdapter
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import kotlinx.android.synthetic.main.fragment_plant_detail.*
import javax.inject.Inject


class PlantDetailFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<PlantDetailViewModel>
    private lateinit var viewModel: PlantDetailViewModel

    private lateinit var photoAdapter: PlantPhotoAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = getViewModel(viewModelFactory) {
            init(
                userId = getFromBundle(userIdKey),
                plantId = getFromBundle(plantIdKey)
            )
        }

        val binding = DataBindingUtil.inflate<FragmentPlantDetailBinding>(
                layoutInflater, R.layout.fragment_plant_detail, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel()
        bindListeners()
        initUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.onDestroyFragment()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_plant_detail, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_measurement-> {
                onClickAddMeasurements(null)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

    private fun initUi() {
        fun onClickPhoto() { /* TODO: Clicking on the photo show it fullscreen */ }
        fun onClickDeletePhoto() = viewModel.onDeletePhoto(plantPhotoPager.currentItem)
        fun onClickRetakePhoto() = viewModel.onRetakePhoto()
        fun onClickAddPhoto(photoType: PlantPhoto.Type) = viewModel.onAddPhoto(photoType)
        fun onNewPhotoType(photoType: PlantPhoto.Type) = viewModel.onUpdatePhotoType(plantPhotoPager.currentItem, photoType)

        photoAdapter = PlantPhotoAdapter(
                ::onClickPhoto,
                ::onClickDeletePhoto,
                ::onClickRetakePhoto,
                ::onClickAddPhoto,
                ::onNewPhotoType)
        plantPhotoPager.adapter = photoAdapter
    }

    private fun bindViewModel() {
        with(viewModel) {
            plantType.observe(viewLifecycleOwner, Observer { plantTypeButton.setPlantType(it) })
            photoData.observe(viewLifecycleOwner, Observer { photoAdapter.submitList(it) })
            startPhotoIntent.observe(viewLifecycleOwner, Observer { startPhotoIntent(it) })
            showPlantDeletedToast.observe(viewLifecycleOwner, Observer { showToast(R.string.plant_deleted) })
        }
    }

    private fun bindListeners() {
        plantTypeButton.setListener(viewModel::onNewPlantType)
        editPlantNameButton.setOnClickListener(::onClickEditNames)
        overflowButton.setOnClickListener(::onClickOverflow)
        addMeasurementButton.setOnClickListener(::onClickAddMeasurements)
        updateLocationButton.setOnClickListener { showSnackbar(getString(R.string.update_location)) }
        deleteButton.setOnClickListener(::onClickDelete)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickEditNames(view: View) {
        viewModel.plant.value?.run {
            EditPlantNameDialog(
                    commonName.orEmpty(),
                    scientificName.orEmpty(),
                    viewModel::onUpdatePlantNames
            ).show(parentFragmentManager,"tag")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickAddMeasurements(view: View?) {
        viewModel.plant.value?.type?.let { plantType ->
            InputMeasurementsDialog(
                    R.string.add_plant_measurements,
                    plantType,
                    null,
                    null,
                    null,
                    viewModel::onMeasurementsAdded
            ).show(parentFragmentManager, "tag")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickOverflow(view: View) {
        overflowButton.isVisible = false
        addMeasurementButton.isVisible = true
        updateLocationButton.isVisible = true
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickDelete(view: View) {
        AlertDialog.Builder(activity).apply {
            setTitle(getString(R.string.delete_plant))
            setMessage(getString(R.string.delete_plant_confirm))
            setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.markPlantForDeletion()
                showToast(getString(R.string.plant_deleted))
                navigateBack()
            }
            setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
            create()
        }.show()
    }
}