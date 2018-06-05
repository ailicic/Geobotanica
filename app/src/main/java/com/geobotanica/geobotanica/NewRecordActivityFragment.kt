package com.geobotanica.geobotanica

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_new_record.*
import timber.log.Timber
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.support.v4.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/**
 * A placeholder fragment containing a simple view.
 */
class NewRecordActivityFragment : Fragment() {

    private val requestTakePhoto = 1
    private lateinit var photoFilePath: String
    private var oldPhotoFilePath: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_record, container, false)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        // Context is now available
        Timber.d("onAttach()")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // View elements are now available
        Timber.d("onViewCreated")


        takePhotoButton.setOnClickListener {
            Timber.d("Starting photo capture intent...")
            val capturePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            capturePhotoIntent.resolveActivity(context?.packageManager) ?: return@setOnClickListener

            try {
                val photoFile = createImageFile()
                context?.let {
                    val authorities = "${it.packageName}.fileprovider"
                    Timber.d("authorities = $authorities")
                    val photoUri: Uri? = FileProvider.getUriForFile(it, authorities, photoFile)
                    Timber.d("photoUri = ${photoUri?.path}")
                    capturePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(capturePhotoIntent, requestTakePhoto)
                }
            } catch (e: IOException) {
                Toast.makeText(context, "Error creating file", Toast.LENGTH_SHORT).show()
            }
        }

        gpsButton.setOnClickListener {
            Toast.makeText(context, "Save position", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            requestTakePhoto -> {
                if (resultCode == RESULT_OK) {
                    Timber.v("New photo received")
                    plantPhoto.setImageBitmap(getScaledBitmap())
                    if (oldPhotoFilePath.isNotEmpty()) {
                        Timber.v("Deleting old photo: $oldPhotoFilePath")
                        val file = File(oldPhotoFilePath)
                        val deleted = file.delete()
                        Timber.v("Old photo deleted = $deleted")
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
