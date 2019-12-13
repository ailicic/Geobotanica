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
import androidx.lifecycle.lifecycleScope
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


class PlantDetailFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<PlantDetailViewModel>
    private lateinit var viewModel: PlantDetailViewModel

    @Inject lateinit var storageHelper: StorageHelper

    private lateinit var photoAdapter: PlantPhotoAdapter

    private var isPhotoRetake: Boolean = false
    private lateinit var newPhotoType: PlantPhoto.Type
    private lateinit var newPhotoUri: String

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
            plantId = getFromBundle(plantIdKey)
            userId = getFromBundle(userIdKey)
        }

        val binding = DataBindingUtil.inflate<FragmentPlantDetailBinding>(
                layoutInflater, R.layout.fragment_plant_detail, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPlantTypeButton()
        initPhotoViewPager()
        bindClickListeners()
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
                Activity.RESULT_OK -> {
                    val photoIndex = plantPhotoPager.currentItem
                    Lg.d("onActivityResult: RESULT_OK (New photo received)")
                    if (isPhotoRetake) {
                        viewModel.updatePlantPhoto(photoIndex, newPhotoUri)
                    } else {
                        viewModel.addPlantPhoto(newPhotoType, newPhotoUri)
                        lifecycleScope.launch(Dispatchers.Main) {
                            delay(300)
                            plantPhotoPager.setCurrentItem(viewModel.photos.value!!.size, true)
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

    private fun initPlantTypeButton() {
        viewModel.plantType.observe(this, Observer {
            plantTypeButton.init(it)
            viewModel.plantType.removeObservers(this)
        })
        plantTypeButton.onNewPlantType = { viewModel.updatePlantType(it) }
    }

    private fun initPhotoViewPager() {
        photoAdapter = PlantPhotoAdapter(
                ::onClickPhoto,
                ::onDeletePhoto,
                ::onRetakePhoto,
                ::onAddPhoto,
                ::onNewPhotoType)
        viewModel.photoData.observe(this, Observer {
            Lg.d("Updating photoData: $it")
            photoAdapter.submitList(it)
        })
        plantPhotoPager.adapter = photoAdapter
    }

    private fun onClickPhoto() {
        // TODO: Clicking on the photo should blow it up
    }

    private fun onDeletePhoto() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.deletePlantPhoto(plantPhotoPager.currentItem)
            delay(300)
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

    private fun onNewPhotoType(photoType: PlantPhoto.Type) {
        viewModel.updatePlantPhotoType(plantPhotoPager.currentItem, photoType)
    }

    private fun bindClickListeners() {
        editPlantNameButton.setOnClickListener(::onClickEditNames)
        overflowButton.setOnClickListener(::onClickOverflow)
        addMeasurementButton.setOnClickListener(::onClickAddMeasurements)
        updateLocationButton.setOnClickListener { showSnackbar("Update location") }
        deleteButton.setOnClickListener(::onClickDelete)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickEditNames(view: View) {
        with(viewModel.plant.value!!) {
            EditPlantNameDialog(
                    commonName.orEmpty(),
                    scientificName.orEmpty(),
                    viewModel::updatePlantNames
            )
        }.show(requireFragmentManager(),"tag")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickAddMeasurements(view: View?) {
        with(viewModel) {
            InputMeasurementsDialog(
                    R.string.add_plant_measurements,
                    plantType.value!!,
                    null,
                    null,
                    null,
                    ::onMeasurementsAdded
            ).show(requireFragmentManager(), "tag")
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
                viewModel.isPlantMarkedForDeletion = true
                showToast(getString(R.string.plant_deleted))
                navigateBack()
            }
            setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
            create()
        }.show()
    }
}