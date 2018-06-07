@file:Suppress("DEPRECATION")

package com.geobotanica.geobotanica

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.fragment_new_record.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class NewRecordFragment : Fragment() {
    private val requestTakePhoto = 1
    private val requestFineLocationPermission = 2
    private lateinit var photoFilePath: String
    private var oldPhotoFilePath: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_record, container, false)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Lg.d("onAttach()")
        // Context is now available

        if (isGpsPermitted()) {
            Lg.d("GPS already permitted")
            requestGpsUpdates()
        } else
            Lg.d("Requesting GPS permissions now...")
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestFineLocationPermission)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // View elements are now available
        Lg.d("onViewCreated()")

        takePhotoButton.setOnClickListener(::takePhotoButtonClickListener)
        gpsButton.setOnClickListener {
            Toast.makeText(context, "Save position", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Lg.d("onDestroyView()")
        takePhotoButton.setOnClickListener(null)
        gpsButton.setOnClickListener(null)
    }

    override fun onDetach() {
        super.onDetach()
        Lg.d("onDetach()")
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

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            requestFineLocationPermission -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Lg.d("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_GRANTED")
                    requestGpsUpdates()
                } else {
                    Lg.d("onRequestPermissionsResult(): permission.ACCESS_FINE_LOCATION: PERMISSION_DENIED")
                }
            }
            else -> { } // Ignore all other requests.
        }
    }

    private inner class GpsLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            with(location) {
                val satellites = extras.getInt("satellites")  // Not used here. See GPS status listeners
                Lg.d("GpsLocationListener(): Accuracy = $accuracy, Satellites = $satellites, " +
                        "Lat = $latitude, Long = $longitude, Alt = $altitude")
                precisionText.text = getString(R.string.precision, accuracy)
            }
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {   Lg.d("GpsLocation: OnStatusChanged") }
        override fun onProviderEnabled(provider: String) {                              Lg.d("GpsLocation: OnProviderEnabled") }
        override fun onProviderDisabled(provider: String) {                             Lg.d("GpsLocation: OnProviderDisabled") }

    }

    // TODO: Check if all GPS related callbacks/listeners need to be unregistered in fragment lifecycle
    //        val locationManager: LocationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    //        locationManager.removeUpdates(GpsLocationListener)

    @RequiresApi(Build.VERSION_CODES.N)
    private inner class GnssStatusCallback : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus?) {
            super.onSatelliteStatusChanged(status)
            val satellitesVisible = status!!.satelliteCount
            var satellitesInUse = 0
            for(i in 0 until satellitesVisible) {
                satellitesInUse += if(status.usedInFix(i)) 1 else 0
            }
            Lg.d("GnssStatus.Callback::onSatelliteStatusChanged(): $satellitesInUse/$satellitesVisible")
            satellitesText?.text = getString(R.string.satellites, satellitesInUse, satellitesVisible)
        }

        override fun onStarted() {
            super.onStarted()
            Lg.d("GnssStatus.Callback::onStarted()")
        }

        override fun onFirstFix(ttffMillis: Int) {
            super.onFirstFix(ttffMillis)
            Lg.d("GnssStatus.Callback::onFirstFix()")
        }

        override fun onStopped() {
            super.onStopped()
            Lg.d("GnssStatus.Callback::onStopped()")
        }
    }


    @SuppressLint("MissingPermission")
    private fun onGpsStatusChanged(event: Int) {
        when (event) {
            GpsStatus.GPS_EVENT_STARTED-> Lg.d("GPS_EVENT_STARTED")
            GpsStatus.GPS_EVENT_STOPPED-> Lg.d("GPS_EVENT_STOPPED")
            GpsStatus.GPS_EVENT_FIRST_FIX-> Lg.d("GPS_EVENT_FIRST_FIX")
            GpsStatus.GPS_EVENT_SATELLITE_STATUS-> {
                val lm: LocationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val status = lm.getGpsStatus(null)
                val satellitesInUse = status.satellites.filter({it.usedInFix()}).count()
                val satellitesVisible = status.satellites.count()
                Lg.d("GPS_EVENT_SATELLITE_STATUS: $satellitesInUse/$satellitesVisible")
                satellitesText?.text = getString(R.string.satellites, satellitesInUse, satellitesVisible)
            }
        }
    }


    private fun isGpsPermitted(): Boolean {
        return ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    // TODO: Verify the implications of this request. It appears it also triggers the permission request.
    @SuppressLint("MissingPermission")
    private fun requestGpsUpdates() {
        Lg.d("Requesting GPS updates now...")
        val lm: LocationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, GpsLocationListener())

         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
             Lg.d("Registering GPS status (API >= 24)")
             lm.registerGnssStatusCallback(GnssStatusCallback())
         } else {
             Lg.d("Registering GPS status (API < 24)")
             lm.addGpsStatusListener(::onGpsStatusChanged)
         }


    }

    @Suppress("UNUSED_PARAMETER")
    private fun takePhotoButtonClickListener(v: View) {
        Lg.d("Starting photo capture intent...")
        val capturePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        capturePhotoIntent.resolveActivity(context?.packageManager) ?: return@takePhotoButtonClickListener

        try {
            val photoFile = createImageFile()
            val authorities = "${context!!.packageName}.fileprovider"
            Lg.d("authorities = $authorities")
            val photoUri: Uri? = FileProvider.getUriForFile(context!!, authorities, photoFile)
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
