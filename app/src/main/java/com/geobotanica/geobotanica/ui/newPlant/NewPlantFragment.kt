@file:Suppress("DEPRECATION")

package com.geobotanica.geobotanica.ui.newPlant

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.Photo
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.User
import com.geobotanica.geobotanica.data.repo.LocationRepo
import com.geobotanica.geobotanica.data.repo.PhotoRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.data.repo.UserRepo
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.activity_new_plant.*
import kotlinx.android.synthetic.main.fragment_new_plant.*
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
    @Inject lateinit var locationService: LocationService
//    @Inject lateinit  var cameraService: CameraService

    private lateinit var user: User
    private val requestFineLocationPermission = 1
    private var currentLocation: Location? = null
    private val requestTakePhoto = 2
    private var photoFilePath: String = ""
    private var oldPhotoFilePath: String = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Lg.d("NewPlantFragment: onAttach()")
        (getActivity() as BaseActivity).activityComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lg.d("NewPlantFragment: onCreate()")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Lg.d("NewPlantFragment: onCreateView()")

        val userId = activity.intent. getLongExtra(getString(R.string.extra_user_id), 0L)
        user = userRepo.get(userId)
        Lg.d("User = $user (id=${user.id})")

        // TODO: Try to push this code into LocationService.
        if (ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Lg.d("NewPlantFragment: GPS permissions already available. Subscribing now...")
            locationService.subscribe(::onLocation)
        } else {
            Lg.d("NewPlantFragment: Requesting GPS permissions now...")
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestFineLocationPermission)
        }

        return inflater.inflate(R.layout.fragment_new_plant, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // View elements are now available
        Lg.d("NewPlantFragment: onViewCreated()")


        takePhotoButton.setOnClickListener(::takePhotoButtonClickListener)
        gpsSwitch.setOnCheckedChangeListener(::onToggleHoldPosition)
    }

    override fun onStart() {
        super.onStart()
        Lg.d("NewPlantFragment: onStart()")
        activity.savePlantButton.setOnClickListener(::onSaveButtonPressed)
    }

    override fun onPause() {
        super.onPause()
        Lg.d("NewPlantFragment: onPause()")
    }

    override fun onResume() {
        super.onResume()
        Lg.d("NewPlantFragment: onResume()")
    }

    override fun onStop() {
        super.onStop()
        Lg.d("onStop()")
        takePhotoButton.setOnClickListener(null)
        gpsSwitch.setOnClickListener(null)
        locationService.unsubscribe(::onLocation)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        Lg.d("NewPlantFragment: onDestroyView()")
    }

    override fun onDetach() {
        super.onDetach()
        Lg.d("NewPlantFragment: onDetach()")
    }

    // TODO: Move to MapFragment
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            requestFineLocationPermission -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Lg.d("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_GRANTED")
                    locationService.subscribe(::onLocation)
                } else {
                    Lg.d("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_DENIED")
                }
            }
            else -> { } // Ignore all other requests.
        }
    }

    fun onToggleHoldPosition(buttonView: CompoundButton , isChecked: Boolean) {
        Lg.d("onToggleHoldPosition(): isChecked=$isChecked")
        if (isChecked)
            locationService.unsubscribe(::onLocation)
        else
            locationService.subscribe(::onLocation)
    }

    private fun onLocation(location: Location) {
        currentLocation = location
        with(location) {
            Lg.d("NewPlantFragment: onLocation(): " +
                    "Used satellitesInUse = ${satellitesInUse ?: ""}, " +
                    "Visible satellitesInUse = ${satellitesInUse ?: ""}, " +
                    "Precision = ${precision ?: ""}, " +
                    "Lat = ${latitude ?: ""}, " +
                    "Long = ${longitude ?: ""}, " +
                    "Alt = ${altitude ?: ""}")

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

    private fun onSaveButtonPressed(view: View) {
        Lg.d("NewPlantFragment: onSaveButtonPressed()")

        val plantType = plantTypeSpinner.selectedItem.toString()
        val photoType = photoTypeSpinner.selectedItem.toString()
        val commonName = commonNameEditText.editText!!.text.toString()
        val latinName = latinNameEditText.editText!!.text.toString()
        if (plantType.isEmpty()) {
            Snackbar.make(view, "Select the plant type", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }
        if (photoFilePath.isEmpty()) {
            Snackbar.make(view, "Take a photo of the plant", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }
        if (!gpsSwitch.isEnabled) {
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

        val plant = Plant(user.id, plantType, commonName, latinName)
        plant.id = plantRepo.insert(plant)
        Lg.d("Plant: $plant (id=${plant.id})")

        val photo = Photo(user.id, plant.id, photoFilePath, photoType)
        photo.id = photoRepo.insert(photo)
        Lg.d("Photo: $photo (id=${photo.id})")

        currentLocation?.let {
            it.plantId = plant.id
            it.id = locationRepo.insert(it)
            Lg.d("Location: $it (id=${it.id})")
        }

        Toast.makeText(appContext, "Plant saved", Toast.LENGTH_SHORT).show()
        activity.finish()
    }
}

