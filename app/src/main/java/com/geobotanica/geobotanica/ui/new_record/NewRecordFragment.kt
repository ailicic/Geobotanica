@file:Suppress("DEPRECATION")

package com.geobotanica.geobotanica.ui.new_record

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
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.location.LocationService
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.fragment_new_record.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class NewRecordFragment : BaseFragment() {
    @Inject lateinit var locationService: LocationService
//    @Inject lateinit  var cameraService: CameraService

    private val requestFineLocationPermission = 1
    private val requestTakePhoto = 2
    private lateinit var photoFilePath: String
    private var oldPhotoFilePath: String = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Lg.d("NewRecordFragment: onAttach()")
        (getActivity() as BaseActivity).activityComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lg.d("NewRecordFragment: onCreate()")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Lg.d("NewRecordFragment: onCreateView()")


        // TODO: Try to push this code into LocationService.
        if(ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Lg.d("NewRecordFragment: GPS permissions already available. Subscribing now...")
            locationService.subscribe(::onLocation)
        } else {
            Lg.d("NewRecordFragment: Requesting GPS permissions now...")
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestFineLocationPermission)
        }

        return inflater.inflate(R.layout.fragment_new_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // View elements are now available
        Lg.d("NewRecordFragment: onViewCreated()")

        takePhotoButton.setOnClickListener(::takePhotoButtonClickListener)
        gpsButton.setOnClickListener {
            Toast.makeText(context, "Save position", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        Lg.d("NewRecordFragment: onStart()")
    }

    override fun onPause() {
        super.onPause()
        Lg.d("NewRecordFragment: onPause()")
    }

    override fun onResume() {
        super.onResume()
        Lg.d("NewRecordFragment: onResume()")
    }

    override fun onStop() {
        super.onStop()
        Lg.d("onStop()")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Lg.d("NewRecordFragment: onDestroyView()")
        takePhotoButton.setOnClickListener(null)
        gpsButton.setOnClickListener(null)
    }

    override fun onDetach() {
        super.onDetach()
        Lg.d("NewRecordFragment: onDetach()")
        locationService.unsubscribe(::onLocation)
    }

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

    private fun onLocation(location: Location) {
        with(location) {
            Lg.d("NewRecordFragment: onLocation(): " +
                    "Used satellitesInUse = ${satellitesInUse ?: ""}, " +
                    "Visible satellitesInUse = ${satellitesInUse ?: ""}, " +
                    "Precision = ${precision ?: ""}, " +
                    "Lat = ${latitude ?: ""}, " +
                    "Long = ${longitude ?: ""}, " +
                    "Alt = ${altitude ?: ""}")

            precision?.let { precisionText.text = getString(R.string.precision, precision)}
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

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val fileName: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val storageDir: File? = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        /storage/emulated/0/Android/data/com.geobotanica/files/Pictures
        val image: File = File.createTempFile(fileName, ".jpg", storageDir)
        photoFilePath = image.absolutePath
        return image
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

