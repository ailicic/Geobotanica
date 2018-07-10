package com.geobotanica.geobotanica.ui.plantdetail

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.Measurement
import com.geobotanica.geobotanica.data.entity.Photo
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.addToConstraintLayout
import com.geobotanica.geobotanica.util.pixelsPerDp
import com.geobotanica.geobotanica.util.setScaledBitmap
import kotlinx.android.synthetic.main.activity_plant_detail.*
import kotlinx.android.synthetic.main.fragment_plant_detail.*
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import javax.inject.Inject
import android.databinding.DataBindingUtil
import com.geobotanica.geobotanica.databinding.FragmentPlantDetailBinding


class PlantDetailFragment : BaseFragment() {
    @Inject lateinit var plantDetailViewModelFactory: PlantDetailViewModelFactory
    @Inject lateinit var userRepo: UserRepo
    @Inject lateinit var plantRepo: PlantRepo
    @Inject lateinit var locationRepo: LocationRepo
    @Inject lateinit var photoRepo: PhotoRepo
    @Inject lateinit var measurementRepo: MeasurementRepo

    override val name = this.javaClass.name.substringAfterLast('.')

    private var plantId = 0L
    private lateinit  var plant: Plant
    private lateinit var location: Location
    private var photos = emptyList<Photo>()
    private var measurements = emptyList<Measurement>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (getActivity() as BaseActivity).activityComponent.inject(this)
    }

    // TODO: Show plant type as icon (not shown anywhere yet)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        plantId = activity.intent.getLongExtra(getString(R.string.extra_plant_id), -1)
        Lg.d("Intent extras: plantId=$plantId")
        plantDetailViewModelFactory.plantId = plantId
        val plantDetailViewModel = ViewModelProviders.of(this, plantDetailViewModelFactory).get(PlantDetailViewModel::class.java)
        plantDetailViewModel.plantId = plantId

        val binding = DataBindingUtil.inflate<FragmentPlantDetailBinding>(
                layoutInflater, R.layout.fragment_plant_detail, container, false).apply {
            viewModel = plantDetailViewModel
            setLifecycleOwner(this@PlantDetailFragment)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchData()
        addWidgets()
        bindClickListeners()
    }

    private fun bindClickListeners() {
        deleteButton.setOnClickListener(::onDeleteButtonPressed)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onDeleteButtonPressed(view: View) {
        AlertDialog.Builder(activity).apply {
            setTitle("Delete Plant")
            setMessage("Are you sure you want to delete this plant and its photos?")
            setPositiveButton("Yes") { _, _ ->
                Lg.d("Deleting plant: $plant, Photo count=${photos.size}")
                photos.forEach { File(it.fileName).delete() }
                plantRepo.delete(plant)
                Toast.makeText(context, "Plant deleted", Toast.LENGTH_SHORT).show()
                activity.finish()
            }
            setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            create()
        }.show()
    }

    private fun fetchData() {

        plant = plantRepo.get(plantId)
        location = locationRepo.getPlantLocation(plantId)
        photos = photoRepo.getAllPhotosOfPlant(plantId)
        measurements = measurementRepo.getAllMeasurementsOfPlant(plantId)

        Lg.d("$plant (id=${plant.id})")
        Lg.d("$location (id=${location.id})")
        photos.forEachIndexed { i, photo ->
            Lg.d("Photo #${i + 1}: $photo (id=${photo.id})")
        }
        measurements.forEachIndexed { i, measurement ->
            Lg.d("Measurement #${i + 1}: $measurement (id=${measurement.id})")
        }
    }

    private fun addWidgets() {
        val viewTreeObserver = plantPhoto.viewTreeObserver
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    @Suppress("DEPRECATION")
                    plantPhoto.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    plantPhoto.setScaledBitmap(photos[0].fileName)
                }
            })
        }

        plant.commonName?.let {
            commonNameText.text = plant.commonName
            commonNameText.visibility = View.VISIBLE
        }
        plant.latinName?.let {
            latinNameText.text = plant.latinName
            latinNameText.visibility = View.VISIBLE
        }

        val measurementId = View.generateViewId()
        measurements.forEachIndexed { index, measurement ->
            val measurementText = TextView(activity).apply {
                id = measurementId + index
                Lg.d("MeasurementText #${index + 1}: id=$id")
                text = (resources.getString(R.string.measurement,
                        Measurement.Type.values()[index],
                        measurement.measurement))
                @Suppress("DEPRECATION")
                setTextColor(resources.getColor(R.color.colorBlack))
                addToConstraintLayout(constraintLayout,
                        below = if (index == 0) nameDivider.id else id - 1,
                        startAt = fragment.id,
                        topMarginDp = 4)
            }

            val dateText = TextView(activity).apply {
                id = measurementText.id + 64
                Lg.d("dateText #${index + 1}: id=$id")
                text = (measurement.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE))
                addToConstraintLayout(constraintLayout,
                        topAt = measurementText.id,
                        endAt = fragment.id,
                        topMarginDp = 0)
            }
        }

        if (measurements.isNotEmpty()) {
            val measuredByText = TextView(activity).apply {
                id = R.id.measuredByText
//                text = resources.getString(R.string.measured_by, userRepo.get(measurements[0].userId).nickname)
                addToConstraintLayout(constraintLayout,
                        below = measurementId + measurements.size - 1,
                        endAt = fragment.id,
                        topMarginDp = 4)
            }

            val measurementsDivider = View(activity).apply {
                id = R.id.measurementsDivider
                layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        pixelsPerDp.toInt())
                @Suppress("DEPRECATION")
                setBackgroundColor(resources.getColor(R.color.colorDarkGrey))
                addToConstraintLayout(constraintLayout,
                        below = measuredByText.id,
                        startAt = fragment.id,
                        endAt = fragment.id)
            }
        }

        val locationText = TextView(activity).apply {
            id = R.id.locationText
            text =  resources.getString(R.string.location,
                    location.precision,
                    location.satellitesInUse,
                    location.satellitesVisible)
            addToConstraintLayout(constraintLayout,
                    below = if (measurements.isNotEmpty()) R.id.measurementsDivider else nameDivider.id,
                    endAt = fragment.id)
        }

        val createdByText = TextView(activity).apply {
            id = R.id.createdByText
//            text =  resources.getString(R.string.created_by,
//                    userRepo.get(plant.userId).nickname,
//                    plant.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE))
            addToConstraintLayout(constraintLayout,
                    below = locationText.id,
                    endAt = fragment.id)
        }
    }
}
