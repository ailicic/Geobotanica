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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.PlantPhoto
import com.geobotanica.geobotanica.databinding.FragmentNewPlantConfirmBinding
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.dialog.EditMeasurementsDialog
import com.geobotanica.geobotanica.ui.dialog.EditPlantNameDialog
import com.geobotanica.geobotanica.ui.dialog.ItemListDialog
import com.geobotanica.geobotanica.ui.viewpager.PhotoData
import com.geobotanica.geobotanica.ui.viewpager.PlantPhotoAdapter
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.Measurement
import com.geobotanica.geobotanica.util.getFromBundle
import com.geobotanica.geobotanica.util.getNullableFromBundle
import kotlinx.android.synthetic.main.fragment_new_plant_confirm.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject


// TODO: Break out some parts into separate classes
// PlantTypeButton: encapsulate icon change and expose result as LiveData (EditMeasurementsButton should subscribe too)
// Measurements: Group text and button. Encapsulate dynamic margin adjustments


// TODO: Automatically wire up the measurements text to the plantTypeButton

// TODO: Need to filter phototypes available based on plant type
// Complete only: fungus, grass
// Trunk on tree only

@Suppress("UNUSED_PARAMETER")
class NewPlantConfirmFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantConfirmViewModel>
    private lateinit var viewModel: NewPlantConfirmViewModel


    private var job: Job? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

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
            photos.add( PhotoData(PlantPhoto.Type.COMPLETE, getFromBundle(photoUriKey)))
            plantType.value = Plant.Type.fromFlag(getFromBundle(plantTypeKey))
            commonName.value = getNullableFromBundle(commonNameKey)
            scientificName.value = getNullableFromBundle(scientificNameKey)
            vernacularId = getNullableFromBundle(vernacularIdKey)
            taxonId = getNullableFromBundle(taxonIdKey)
            height.value = getNullableFromBundle(heightMeasurementKey)
            diameter.value = getNullableFromBundle(diameterMeasurementKey)
            trunkDiameter.value = getNullableFromBundle(trunkDiameterMeasurementKey)
            Lg.d("Fragment args: userId=$userId, photoType=$plantType, " +
                    "commonName=$commonName, scientificName=$scientificName, " +
                    "vernId=$vernacularId, taxonId=$taxonId, photos=$photos, " +
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
        addOnBackPressedCallback()
        initPlantTypeButton()
        initPhotoViewPager()
        updateMeasurementsLayout()
        bindClickListeners()
    }

    override fun onStop() {
        super.onStop()
        job?.cancel()
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
                        job = mainScope.launch(Dispatchers.Main) {
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
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            AlertDialog.Builder(activity).apply {
                setTitle(getString(R.string.discard_new_plant))
                setMessage(getString(R.string.discard_new_plant_confirm))
                setPositiveButton(getString(R.string.yes)) { _, _ ->
                    viewModel.photos.forEach {
                        val photoUri = it.photoUri
                        Lg.d("Deleting old photo: $photoUri")
                        Lg.d("Delete photo result = ${File(photoUri).delete()}")
                    }
                    activity.currentLocation = null
                    showToast(getString(R.string.plant_discarded))
                    val navController = activity.findNavController(R.id.fragment)
                    navController.popBackStack(R.id.mapFragment, false)
                }
                setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
                create()
            }.show()
        }
    }

    private fun initPlantTypeButton() {
        plantTypeButton.init(viewModel.plantType.value!!)
        plantTypeButton.onNewPlantType = {
            viewModel.plantType.value = it
            if (it != Plant.Type.TREE) // TODO: Move this out. Observe VM instead.
                viewModel.trunkDiameter.value = null
        }
    }

    private fun initPhotoViewPager() {
        photoAdapter = PlantPhotoAdapter(::onClickPhoto, ::onClickPhotoType, ::onClickDeletePhoto, ::onClickRetakePhoto, ::onClickAddPhoto)
        photoAdapter.items = viewModel.photos
        plantPhotoPager.adapter = photoAdapter
    }

    // ENCAPSULATE AWAY
    private fun updateMeasurementsLayout() {
        var measurementCount = 0
        with(viewModel) {
            height.value?.let { ++measurementCount }
            diameter.value?.let { ++measurementCount }
            trunkDiameter.value?.let { ++measurementCount }
        }
        val layoutParams = editMeasurementsButton.layoutParams as ConstraintLayout.LayoutParams
        if (measurementCount > 2)
            layoutParams.topMargin = dpToPixels(20)
        else
            layoutParams.topMargin = dpToPixels(8)
        editMeasurementsButton.layoutParams = layoutParams
    }

    private fun bindClickListeners() {
        editPlantNameButton.setOnClickListener(::onClickEditNames)
        editMeasurementsButton.setOnClickListener(::onClickEditMeasurements)
        fab.setOnClickListener(::onFabClicked)
    }

    private fun onClickPhoto() {
        // TODO: Clicking on the photo should blow it up
    }

    private fun onClickPhotoType() {
        val photoIndex = plantPhotoPager.currentItem
        ItemListDialog(
                R.string.change_photo_type,
                R.array.photo_type_drawable_array,
                PlantPhoto.Type.values().filter { it != viewModel.photos[photoIndex].photoType },
                ::onChangePhotoType
        ).show(fragmentManager!!,"tag")
    }

    private fun onChangePhotoType(photoType: PlantPhoto.Type) {
        val photoIndex = plantPhotoPager.currentItem
        viewModel.photos[photoIndex].photoType = photoType
//        photoAdapter.notifyItemChanged(photoIndex) // Re-creates view on first change!?
        photoAdapter.notifyDataSetChanged()
    }

    private fun onClickDeletePhoto() {
        photoAdapter.isPhotoMenuVisible = false

        AlertDialog.Builder(activity).apply {
            setTitle(getString(R.string.delete_photo))
            setMessage(getString(R.string.delete_photo_confirm))
            setPositiveButton(getString(R.string.yes)) { _, _ ->
                val photoIndex = plantPhotoPager.currentItem
                val photoUri = viewModel.photos[photoIndex].photoUri
                Lg.d("Deleting old photo: $photoUri (Result = ${File(photoUri).delete()})")
                job = mainScope.launch(Dispatchers.Main) {
                    delay(300)
                    viewModel.photos.removeAt(photoIndex)
                    photoAdapter.notifyItemRemoved(photoIndex)
                    showToast(getString(R.string.photo_deleted))
                }
            }
            setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
            create()
        }.show()
    }

    private fun onClickRetakePhoto() {
        photoAdapter.isPhotoMenuVisible = false

        AlertDialog.Builder(activity).apply {
            setTitle(getString(R.string.retake_photo))
            setMessage(getString(R.string.retake_photo_confirm))
            setPositiveButton(getString(R.string.yes)) { _, _ ->
                isPhotoRetake = true
                val photoFile = createPhotoFile()
                newPhotoUri = photoFile.absolutePath
                startPhotoIntent(photoFile)
            }
            setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
            create()
        }.show()
    }

    private fun onClickAddPhoto() {
        photoAdapter.isPhotoMenuVisible = false
        ItemListDialog(
                R.string.select_new_photo_type,
                R.array.photo_type_drawable_array,
                PlantPhoto.Type.values().toList(),
                ::onNewPhotoType
        ).show(fragmentManager!!,"tag")
    }

    private fun onNewPhotoType(photoType: PlantPhoto.Type) {
        isPhotoRetake = false
        newPhotoType = photoType
        val photoFile = createPhotoFile()
        newPhotoUri = photoFile.absolutePath
        startPhotoIntent(photoFile)
    }

    // ENCAPSULATE AWAY
    private fun onClickEditNames(view: View) {
        with(viewModel) {
            EditPlantNameDialog(
                    commonName.value.orEmpty(),
                    scientificName.value.orEmpty(),
                    ::onNewPlantName
            )
        }.show(fragmentManager!!,"tag")
    }


    // ENCAPSULATE AWAY
    private fun onClickEditMeasurements(view: View) {
        with(viewModel) {
            EditMeasurementsDialog(
                    plantType.value!!,
                    height.value,
                    diameter.value,
                    trunkDiameter.value,
                    ::onNewPlantMeasurements
            )
        }.show(fragmentManager!!,"tag")
    }

    private fun onNewPlantMeasurements(newHeight: Measurement?, newDiameter: Measurement?, newTrunkDiameter: Measurement?) {
        with(viewModel) {
            height.value = newHeight
            diameter.value = newDiameter
            trunkDiameter.value = newTrunkDiameter
        }


        updateMeasurementsLayout() // ENCAPSULATE AWAY
    }

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

