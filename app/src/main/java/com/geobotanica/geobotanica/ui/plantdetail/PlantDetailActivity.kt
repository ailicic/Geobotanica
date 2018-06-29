package com.geobotanica.geobotanica.ui.plantdetail

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import android.widget.Toast
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.R.id.measurementsDivider
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.Measurement
import com.geobotanica.geobotanica.data.entity.Photo
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.addToConstraintLayout
import com.geobotanica.geobotanica.util.pixelsPerDp
import com.geobotanica.geobotanica.util.setScaledBitmap
import kotlinx.android.synthetic.main.activity_plant_detail.*
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import javax.inject.Inject


class PlantDetailActivity : BaseActivity() {
    @Inject lateinit var userRepo: UserRepo
    @Inject lateinit var plantRepo: PlantRepo
    @Inject lateinit var locationRepo: LocationRepo
    @Inject lateinit var photoRepo: PhotoRepo
    @Inject lateinit var measurementRepo: MeasurementRepo

    override val name = this.javaClass.name.substringAfterLast('.')
    private lateinit  var plant: Plant
    private lateinit var location: Location
    private var photos = emptyList<Photo>()
    private var measurements = emptyList<Measurement>()

    // TODO: Show plant type as icon (not shown anywhere yet)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_detail)
        setSupportActionBar(toolbar)

        activityComponent.inject(this)

        bindClickListeners()
        fetchData()
        addWidgets()
    }

    private fun bindClickListeners() {
        deleteButton.setOnClickListener(::onDeleteButtonPressed)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Add new photos/measurements", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onDeleteButtonPressed(view: View) {
        AlertDialog.Builder(this).apply {
            setTitle("Delete Plant")
            setMessage("Are you sure you want to delete this plant and its photos?")
            setPositiveButton("Yes") { _, _ ->
                Lg.d("Deleting plant: $plant, Photo count=${photos.size}")
                photos.forEach { File(it.fileName).delete() }
                plantRepo.delete(plant)
                Toast.makeText(context, "Plant deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            create()
        }.show()
    }

    private fun fetchData() {
        val plantId = intent.getLongExtra(getString(R.string.extra_plant_id), -1)
        Lg.d("Intent extras: plantId=$plantId")

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
            val measurementText = TextView(this)
            measurementText.apply {
                id = measurementId + index
                Lg.d("MeasurementText #${index + 1}: id=$id")
                text = (resources.getString(R.string.measurement,
                        Measurement.Type.values()[index],
                        measurement.measurement))
                @Suppress("DEPRECATION")
                setTextColor(resources.getColor(R.color.colorBlack))
                addToConstraintLayout(constraintLayout,
                        below = if (index == 0) nameDivider.id else id - 1,
                        startAt = constraintLayout.id,
                        topMarginDp = 4)
            }

            val dateText = TextView(this)
            dateText.apply {
                id = measurementText.id + 64
                Lg.d("dateText #${index + 1}: id=$id")
                text = (measurement.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE))
                addToConstraintLayout(constraintLayout,
                        topAt = measurementText.id,
                        endAt = constraintLayout.id,
                        topMarginDp = 0)
            }
        }

        if (measurements.isNotEmpty()) {
            val measuredByText = TextView(this)
            measuredByText.apply {
                id = R.id.measuredByText
                text = resources.getString(R.string.measured_by, userRepo.get(measurements[0].userId).nickname)
                addToConstraintLayout(constraintLayout,
                        below = measurementId + measurements.size - 1,
                        endAt = constraintLayout.id,
                        topMarginDp = 4)
            }

            val measurementsDivider = View(this)
            measurementsDivider.apply {
                id = R.id.measurementsDivider
                layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        pixelsPerDp.toInt())
                @Suppress("DEPRECATION")
                setBackgroundColor(resources.getColor(R.color.colorDarkGrey))
                addToConstraintLayout(constraintLayout,
                        below = measuredByText.id,
                        startAt = constraintLayout.id,
                        endAt = constraintLayout.id)
            }
        }

        val locationText = TextView(this)
        locationText.apply {
            id = R.id.locationText
            text =  resources.getString(R.string.location,
                    location.precision,
                    location.satellitesInUse,
                    location.satellitesVisible)
            addToConstraintLayout(constraintLayout,
                    below = if (measurements.isNotEmpty()) measurementsDivider else nameDivider.id,
                    endAt = constraintLayout.id)
        }

        val createdByText = TextView(this)
        createdByText.apply {
            id = R.id.createdByText
            text =  resources.getString(R.string.created_by,
                    userRepo.get(plant.userId).nickname,
                    plant.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE))
            addToConstraintLayout(constraintLayout,
                    below = locationText.id,
                    endAt = constraintLayout.id)
        }
    }
}
