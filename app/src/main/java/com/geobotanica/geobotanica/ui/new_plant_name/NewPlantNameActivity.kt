package com.geobotanica.geobotanica.ui.new_plant_name

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.CompoundButton
import android.widget.Toast
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Photo
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.repo.LocationRepo
import com.geobotanica.geobotanica.data.repo.PhotoRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.activity_new_plant_name.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*
import javax.inject.Inject

// TODO: Fix white input text
// TODO: Carry location through if held prior
// TODO: Consider using Fragment in activity for Snackbar
class NewPlantNameActivity : BaseActivity() {
    @Inject lateinit var plantRepo: PlantRepo
    @Inject lateinit var locationRepo: LocationRepo
    @Inject lateinit var photoRepo: PhotoRepo

    override val name = this.javaClass.name.substringAfterLast('.')

    private var userId = 0L
    private var plantType = 0
    private var photoFilePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_plant_name)

        activityComponent.inject(this)

        userId = intent.getLongExtra(getString(R.string.extra_user_id), -1L)
        plantType = intent.getIntExtra(getString(R.string.extra_plant_type), -1)
        photoFilePath = intent.getStringExtra(getString(R.string.extra_plant_photo_path))
        Lg.d("Intent extras: userId=$userId, plantType=$plantType, photoFilePath=$photoFilePath")
    }

    override fun onResume() {
        super.onResume()
        val viewTreeObserver = plantPhoto.getViewTreeObserver()
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    plantPhoto.getViewTreeObserver().removeGlobalOnLayoutListener(this)
                    plantPhoto.setImageBitmap(getScaledBitmap())
                }
            })
        }
        measurementsSwitch.setOnCheckedChangeListener(::onToggleAddMeasurement)
        fab.setOnClickListener(::onFabPressed)
    }


    private fun onToggleAddMeasurement(buttonView: CompoundButton, isChecked: Boolean) {
        Lg.d("onToggleHoldPosition(): isChecked=$isChecked")
        if (isChecked) {
            manualRadioButton.isEnabled = true
            assistedRadioButton.isEnabled = true
        } else {
            manualRadioButton.isEnabled = false
            assistedRadioButton.isEnabled = false
        }
    }

    // TODO: Push validation into the repo?
    private fun onFabPressed(view: View) {
        Lg.d("NewPlantFragment: onSaveButtonPressed()")

        val commonName = commonNameEditText.editText!!.text.toString().trim()
        val latinName = latinNameEditText.editText!!.text.toString().trim()

        if (commonName.isEmpty() && latinName.isEmpty()) {
            Snackbar.make(view, "Provide a plant name", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }

        if (measurementsSwitch.isChecked) {
            Toast.makeText(this, "Measurements", Toast.LENGTH_SHORT).show()
        } else {
            if (!gps.gpsSwitch.isEnabled) {
                Snackbar.make(view, "Wait for GPS fix", Snackbar.LENGTH_LONG).setAction("Action", null).show()
                return
            }
            if (!gps.gpsSwitch.isChecked) {
                Snackbar.make(view, "Plant position must be held", Snackbar.LENGTH_LONG).setAction("Action", null).show()
                return
            }
            val plant = Plant(userId, plantType, commonName, latinName)
            plant.id = plantRepo.insert(plant)
            Lg.d("Plant: $plant (id=${plant.id})")

            val photo = Photo(userId, plant.id, Photo.Type.COMPLETE.ordinal, photoFilePath)
            photo.id = photoRepo.insert(photo)
            Lg.d("Photo: $photo (id=${photo.id})")

            gps.currentLocation?.let {
                it.plantId = plant.id
                it.id = locationRepo.insert(it)
                Lg.d("Location: $it (id=${it.id})")
            }

            Toast.makeText(this, "Plant saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getScaledBitmap(): Bitmap {
        val imageViewWidth = plantPhoto.width
        val imageViewHeight = plantPhoto.height

        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(photoFilePath, bmOptions)
        val bitmapWidth = bmOptions.outWidth
        val bitmapHeight = bmOptions.outHeight
        val scaleFactor = Math.min(bitmapWidth/imageViewWidth, bitmapHeight/imageViewHeight)

        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor

        return BitmapFactory.decodeFile(photoFilePath, bmOptions)
    }

}
