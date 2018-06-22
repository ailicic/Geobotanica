@file:Suppress("DEPRECATION")

package com.geobotanica.geobotanica.ui.new_plant

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.*
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.activity_new_plant.*
import kotlinx.android.synthetic.main.fragment_new_plant.*
import kotlinx.android.synthetic.main.gps_compound_view.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class NewPlantFragment : BaseFragment() {
    @Inject lateinit var userRepo: UserRepo
    @Inject lateinit var plantRepo: PlantRepo
    @Inject lateinit var locationRepo: LocationRepo
    @Inject lateinit var photoRepo: PhotoRepo
    @Inject lateinit var measurementRepo: MeasurementRepo
    @Inject lateinit var locationService: LocationService
//    @Inject lateinit  var cameraService: CameraService

    override val name = this.javaClass.name.substringAfterLast('.')
    private lateinit var user: User
    private var currentLocation: Location? = null
    private val requestTakePhoto = 2
    private var photoFilePath: String = ""
    private var oldPhotoFilePath: String = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (getActivity() as BaseActivity).activityComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val userId = activity.intent. getLongExtra(getString(R.string.extra_user_id), 0L)
        user = userRepo.get(userId)
        Lg.d("User = $user (id=${user.id})")


        return inflater.inflate(R.layout.fragment_new_plant, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        takePhotoButton.setOnClickListener(::takePhotoButtonClickListener)
        gpsSwitch.setOnCheckedChangeListener(::onToggleHoldPosition)
    }

    override fun onStart() {
        super.onStart()
        activity.savePlantButton.setOnClickListener(::onSaveButtonPressed)
        locationService.subscribe(::onLocation)
    }

    override fun onStop() {
        super.onStop()
        takePhotoButton.setOnClickListener(null)
        gpsSwitch.setOnClickListener(null)
        locationService.unsubscribe(::onLocation)

    }

    @Suppress("UNUSED_PARAMETER")
    private fun onToggleHoldPosition(buttonView: CompoundButton, isChecked: Boolean) {
        Lg.d("onToggleHoldPosition(): isChecked=$isChecked")
        if (isChecked)
            locationService.unsubscribe(::onLocation)
        else
            locationService.subscribe(::onLocation)
    }

    private fun onLocation(location: Location) {
        currentLocation = location
        with(location) {
            Lg.d("onLocation(): $this")

            precision?.let {
                precisionText.text = getString(R.string.precision, precision)
                gpsSwitch.isEnabled = true
            }
            satellitesInUse?.let { satellitesText?.text = getString(R.string.satellites, satellitesInUse, satellitesVisible) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            requestTakePhoto -> {
                if (resultCode == RESULT_OK) {
                    Lg.d("New photo received")
                    plantPhoto.setImageBitmap(getScaledBitmap())
                    if (oldPhotoFilePath.isNotEmpty()) {
                        Lg.d("Deleting old photo: $oldPhotoFilePath")
                        Lg.d("Delete photo result = ${File(oldPhotoFilePath).delete()}")
                        oldPhotoFilePath = ""
                    }
                    oldPhotoFilePath = photoFilePath
                } else {
                    Toast.makeText(context, "Error taking photo", Toast.LENGTH_SHORT).show()
                }
            }
            else -> Toast.makeText(context, "Unrecognized request code", Toast.LENGTH_SHORT).show()
        }
    }


    @Suppress("UNUSED_PARAMETER")
    private fun takePhotoButtonClickListener(v: View) {
        Lg.d("Starting photo capture intent...")
        val capturePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        capturePhotoIntent.resolveActivity(context?.packageManager) ?: return@takePhotoButtonClickListener

        try {
            val photoFile = createImageFile()
            val authorities = "${appContext.packageName}.fileprovider"
            Lg.d("authorities = $authorities")
            val photoUri: Uri? = FileProvider.getUriForFile(appContext, authorities, photoFile)
            Lg.d("photoUri = ${photoUri?.path}")
            capturePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(capturePhotoIntent, requestTakePhoto)
        } catch (e: IOException) {
            Toast.makeText(context, "Error creating file", Toast.LENGTH_SHORT).show()
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

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val fileName: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val storageDir: File? = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        /storage/emulated/0/Android/data/com.geobotanica/files/Pictures
        val image: File = File.createTempFile(fileName, ".jpg", storageDir)
        photoFilePath = image.absolutePath
        return image
    }

    // TODO: Push validation into the repo?
    private fun onSaveButtonPressed(view: View) {
        Lg.d("NewPlantFragment: onSaveButtonPressed()")

        val plantType = plantTypeSpinner.selectedItemId.toInt() - 1
        val photoType = photoTypeSpinner.selectedItemId.toInt()
        val commonName = commonNameEditText.editText!!.text.toString().trim()
        val latinName = latinNameEditText.editText!!.text.toString().trim()
        val height = heightEditText.text.toString().toFloatOrNull()
        val diameter = diameterEditText.text.toString().toFloatOrNull()
//        val trunkDiameter = trunkDiameterEditText.text.toString().toFloat()
        if (plantType == -1) {
            Snackbar.make(view, "Select the plant type", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }
        if (photoFilePath.isEmpty()) {
            Snackbar.make(view, "Take a photo of the plant", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }
        if (gpsSwitch.isEnabled) {
            Snackbar.make(view, "Wait for GPS fix", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }
        if (!gpsSwitch.isChecked) {
            Snackbar.make(view, "Plant position must be held", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }
        if (commonName.isEmpty() && latinName.isEmpty()) {
            Snackbar.make(view, "Provide a plant name", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }
        if (height == 0F || diameter == 0F) {
            Snackbar.make(view, "Provide plant dimensions", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }

        val plant = Plant(user.id, plantType, commonName, latinName)
        plant.id = plantRepo.insert(plant)
        Lg.d("Plant: $plant (id=${plant.id})")

        val photo = Photo(user.id, plant.id, photoType, photoFilePath)
        photo.id = photoRepo.insert(photo)
        Lg.d("Photo: $photo (id=${photo.id})")

        currentLocation?.let {
            it.plantId = plant.id
            it.id = locationRepo.insert(it)
            Lg.d("Location: $it (id=${it.id})")
        }

        // TODO: Handle units (default units should be in settings, always store cm, present user default)
        height?.let { measurementRepo.insert(Measurement(user.id, plant.id, Measurement.Type.HEIGHT.ordinal, height)) }
        diameter?.let { measurementRepo.insert(Measurement(user.id, plant.id, Measurement.Type.DIAMETER.ordinal, diameter)) }
        measurementRepo.getAllMeasurementsOfPlant(plant.id).forEach {
            Lg.d("${Measurement.Type.values()[it.type]}=${it.measurement} cm, userId=${it.userId}, plantId=${it.plantId}, id=${it.id}")
        }

        Toast.makeText(appContext, "Plant saved", Toast.LENGTH_SHORT).show()
        activity.finish()
    }
}

