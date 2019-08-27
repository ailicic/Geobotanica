package com.geobotanica.geobotanica.ui

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// NavBundle keys
const val plantNameFilterOptionsKey = "plantNameFilterOptions" // Used by PlantNameFilterDialog : DialogFragment()

abstract class BaseFragment : Fragment() {
    @Inject lateinit var appContext: Context
    @Inject lateinit var activity: MainActivity
    @Inject lateinit var defaultSharedPrefs: SharedPreferences

    private val className: String by lazy { this.toString().substringBefore('{') }
    val sharedPrefs: SharedPreferences by lazy { // Each fragment has private sharedPrefs
        activity.getSharedPreferences(className, Context.MODE_PRIVATE)
    }

    // NavBundle keys
    protected val userIdKey = "userId"
    protected val plantIdKey = "plantId"
    protected val plantTypeKey = "photoType"
    protected val taxonIdKey = "taxonId"
    protected val vernacularIdKey = "vernacularId"
    protected val commonNameKey = "commonName"
    protected val scientificNameKey = "plantScientificName"
    protected val photoUriKey = "plantPhoto"
    protected val locationKey = "location"
    protected val heightMeasurementKey = "heightMeasurement"
    protected val diameterMeasurementKey = "diameterMeasurement"
    protected val trunkDiameterMeasurementKey = "trunkDiameterMeasurement"

    // Default SharedPrefs keys
    protected val sharedPrefsIsFirstRunKey = "isFirstRun"

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

    protected fun showToast(stringResId: Int) = showToast(getString(stringResId))

    protected fun showToast(message: String) = Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

    protected fun showSnackbar(message: String, button: String = "", action: ((View) -> Unit)? = null) {
        Snackbar.make(this.view!!, message, Snackbar.LENGTH_LONG).run {
            action?.let { setAction(button, action) }
            show()
        }
    }

    protected fun showSnackbar(stringId: Int, buttonStringId: Int = 0, action: ((View) -> Unit)? = null) {
        val buttonString = if (buttonStringId != 0) getString(buttonStringId) else ""
        showSnackbar(resources.getString(stringId), buttonString, action)
    }

    // TODO: Remove after better approach to create test images
    protected fun fileFromDrawable(resId: Int, filename: String): String {
        val bitmap = BitmapFactory.decodeResource(resources, resId)
        val extStorageDir = Environment.getExternalStorageDirectory().toString()
        val file = File(extStorageDir, "$filename.png")
        val outStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        outStream.flush()
        outStream.close()
        return file.absolutePath
    }

    fun startPhotoIntent(photoFile: File) {
        val capturePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        capturePhotoIntent.resolveActivity(activity.packageManager)

        try {
            val authorities = "${activity.packageName}.fileprovider"
//            Lg.v("authorities = $authorities")
            val photoUri: Uri? = FileProvider.getUriForFile(activity, authorities, photoFile) // Adds 12 digits to filename
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