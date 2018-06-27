package com.geobotanica.geobotanica.ui.plantdetail

import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.design.widget.Snackbar
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.Measurement
import com.geobotanica.geobotanica.data.entity.Photo
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.setScaledBitmap
import kotlinx.android.synthetic.main.activity_plant_detail.*
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject


class PlantDetailActivity : BaseActivity() {
    @Inject lateinit var userRepo: UserRepo
    @Inject lateinit var plantRepo: PlantRepo
    @Inject lateinit var locationRepo: LocationRepo
    @Inject lateinit var photoRepo: PhotoRepo
    @Inject lateinit var measurementRepo: MeasurementRepo

    override val name = this.javaClass.name.substringAfterLast('.')
    private var userId = 0L
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

        userId = intent.getLongExtra(getString(R.string.extra_user_id), -1L)
        val plantId = intent.getLongExtra(getString(R.string.extra_plant_id), -1)
        Lg.d("Intent extras: userId=$userId, plantId=$plantId")

        plant = plantRepo.get(plantId)
        location = locationRepo.getPlantLocation(plantId)
        photos = photoRepo.getAllPhotosOfPlant(plantId)
        measurements = measurementRepo.getAllMeasurementsOfPlant(plantId)

        Lg.d("$plant (id=${plant.id})")
        Lg.d("$location (id=${location.id})")
        photos.forEachIndexed { i, photo ->
            Lg.d("Photo #${i+1}: $photo (id=${photo.id})")
        }
        measurements.forEachIndexed { i, measurement ->
            Lg.d("Measurement #${i+1}: $measurement (id=${measurement.id})")
        }

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Add new photos/measurements", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

    override fun onStart() {
        super.onStart()

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


        val fourDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f,
                resources.displayMetrics).toInt()
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

                constraintLayout.addView(this)

                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)

                if (index == 0) {
                    constraintSet.connect(id, ConstraintSet.TOP, R.id.nameDivider, ConstraintSet.BOTTOM, fourDp)
                } else {
                    constraintSet.connect(id, ConstraintSet.TOP, id - 1, ConstraintSet.BOTTOM, fourDp)

                }
                constraintSet.connect(id, ConstraintSet.LEFT, constraintLayout.id, ConstraintSet.LEFT, 4 * fourDp)
                constraintSet.applyTo(constraintLayout)
            }

            val dateText = TextView(this)
            dateText.apply {
                id = measurementText.id + 64
                Lg.d("dateText #${index + 1}: id=$id")
                text = (measurement.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE))

                constraintLayout.addView(this)

                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(id, ConstraintSet.TOP, measurementText.id, ConstraintSet.TOP)
                constraintSet.connect(id, ConstraintSet.RIGHT, constraintLayout.id, ConstraintSet.RIGHT, 4 * fourDp)
                constraintSet.applyTo(constraintLayout)
            }
        }

        if (measurements.isNotEmpty()) {
            val measuredByText = TextView(this)
            measuredByText.apply {
                id = R.id.measuredByText
                text = resources.getString(R.string.measured_by, userRepo.get(measurements[0].userId).nickname)

                constraintLayout.addView(this)

                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(id, ConstraintSet.TOP, measurementId + measurements.size - 1, ConstraintSet.BOTTOM)
                constraintSet.connect(id, ConstraintSet.RIGHT, constraintLayout.id, ConstraintSet.RIGHT, 4 * fourDp)
                constraintSet.applyTo(constraintLayout)
            }

            val measurementsDivider = View(this)
            measurementsDivider.apply {
                id = R.id.measurementsDivider
                layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        fourDp / 4)
                @Suppress("DEPRECATION")
                setBackgroundColor(resources.getColor(R.color.colorDarkGrey))

                constraintLayout.addView(this)

                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(id, ConstraintSet.TOP, R.id.measuredByText, ConstraintSet.BOTTOM, 2 * fourDp)
                constraintSet.connect(id, ConstraintSet.RIGHT, constraintLayout.id, ConstraintSet.RIGHT, 4 * fourDp)
                constraintSet.connect(id, ConstraintSet.LEFT, constraintLayout.id, ConstraintSet.LEFT, 4 * fourDp)
                constraintSet.applyTo(constraintLayout)
            }
        }
        val createdByText = TextView(this)
        createdByText.apply {
            id = R.id.createdByText
            text =  resources.getString(R.string.created_by,
                    userRepo.get(plant.userId).nickname,
                    plant.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE))

            constraintLayout.addView(this)

            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            if ( measurements.isEmpty())
                constraintSet.connect(id, ConstraintSet.TOP, R.id.nameDivider, ConstraintSet.BOTTOM, 2 * fourDp)
            else
                constraintSet.connect(id, ConstraintSet.TOP, R.id.measurementsDivider, ConstraintSet.BOTTOM, 2 * fourDp)
            constraintSet.connect(id, ConstraintSet.RIGHT, constraintLayout.id, ConstraintSet.RIGHT, 4 * fourDp)
            constraintSet.applyTo(constraintLayout)

        }
//        val modifiedByText = TextView(this)
    }
}
