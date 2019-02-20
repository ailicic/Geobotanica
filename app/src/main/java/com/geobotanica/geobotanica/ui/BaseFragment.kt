package com.geobotanica.geobotanica.ui

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.fragment.app.Fragment
import com.geobotanica.geobotanica.util.Lg
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

abstract class BaseFragment : Fragment() {
    @Inject lateinit var appContext: Context
    @Inject lateinit var activity: MainActivity
    @Inject lateinit var defaultSharedPrefs: SharedPreferences

    val className: String by lazy { this.toString().substringBefore('{') }
    val sharedPrefs: SharedPreferences by lazy {
        activity.getSharedPreferences(className, Context.MODE_PRIVATE)
    }

    // NavBundle/SharedPrefs keys
    protected val userIdKey = "userId"
    protected val plantIdKey = "plantId"
    protected val plantTypeKey = "plantType"
    protected val commonNameKey = "commonName"
    protected val latinNameKey = "plantLatinName"
    protected val photoUriKey = "plantPhoto"
    protected val locationKey = "location"
    protected val heightMeasurementKey = "heightMeasurement"
    protected val diameterMeasurementKey = "diameterMeasurement"
    protected val trunkDiameterMeasurementKey = "trunkDiameterMeasurement"

    protected val requestTakePhoto = 1

    protected fun requestPermission(permission: String) {
        lazy { this }.run {
            requestPermissions(arrayOf(permission), getRequestCode(permission))
        }
    }

    protected fun wasPermissionGranted(permission: String) =
        ContextCompat.checkSelfPermission(activity, permission) == PERMISSION_GRANTED

    protected fun getRequestCode(permission: String) = when (permission) {
        ACCESS_FINE_LOCATION -> 1
        WRITE_EXTERNAL_STORAGE -> 2
        else -> 0
    }

    protected fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    protected fun showSnackbar(message: String, button: String = "", action: ((View) -> Unit)? = null) {
        Snackbar.make(this.view!!, message, Snackbar.LENGTH_LONG).run {
            action?.let { setAction(button, action) }
            show()
        }
    }

    protected fun showSnackbar(stringId: Int, button: String = "", action: ((View) -> Unit)? = null) {
        showSnackbar(resources.getString(stringId), button, action)
    }

    fun createPhotoFile(): File {
        val fileName: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
//        Lg.d("createPhotoFile(): fileName=$fileName")
        val storageDir: File? = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        /storage/emulated/0/Android/data/com.geobotanica/files/Pictures
        return File.createTempFile(fileName, ".jpg", storageDir)
    }

    fun startPhotoIntent(photoFile: File) {
        val capturePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        capturePhotoIntent.resolveActivity(activity.packageManager)

        try {
            val authorities = "${activity.packageName}.fileprovider"
//            Lg.v("authorities = $authorities")
            val photoUri: Uri? = FileProvider.getUriForFile(activity, authorities, photoFile) // Adds 12 digits to filename
            Lg.d("photoUri = ${photoUri?.path}")
            capturePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(capturePhotoIntent, requestTakePhoto)

        } catch (e: IOException) {
            showToast("Photo not captured")
        }
    }



    override fun onAttach(context: Context) {
        super.onAttach(context)
        Lg.v("$className: onAttach()")
        (context as MainActivity).applicationComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lg.v("$className: onCreate()")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Lg.v("$className: onCreateView()")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Lg.v("$className: onViewCreated()")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Lg.v("$className: onActivityCreated()")
    }

    override fun onStart() {
        super.onStart()
        Lg.v("$className: onStart()")
    }

    override fun onResume() {
        super.onResume()
        Lg.v("$className: onResume()")
    }

    override fun onPause() {
        super.onPause()
        Lg.v("$className: onPause()")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Lg.v("$className: onSaveInstanceState()")
    }

    override fun onStop() {
        super.onStop()
        Lg.v("$className: onStop()")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Lg.v("$className: onDestroyView()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Lg.v("$className: onDestroy()")
    }

    override fun onDetach() {
        super.onDetach()
        Lg.v("$className: onDetach()")
    }
}