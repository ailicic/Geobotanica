package com.geobotanica.geobotanica.ui.newplantphoto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.fragment_new_plant_type.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class NewPlantPhotoFragment : BaseFragment() {
    override val className = this.javaClass.name.substringAfterLast('.')
    override val sharedPrefsKey = "newPlantPhotoSharedPrefs"

    private var userId = 0L
    private var plantType = 0
    private var location: Location? = null
    private val requestTakePhoto = 2
    private var photoUri: String = ""
    private var oldPhotoUri: String = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
//        plantDetailViewModelFactory.plantId = plantId
//        viewModel = ViewModelProviders.of(this, plantDetailViewModelFactory).get(PlantDetailViewModel::class.java)

//        val binding = DataBindingUtil.inflate<FragmentPlantDetailBinding>(
//                layoutInflater, R.layout.fragment_new_plant_type, container, false).apply {
//            viewModel = this@NewPlantTypeFragment.viewModel
//            setLifecycleOwner(this@NewPlantTypeFragment)
//        }
//        return binding.root
        return inflater.inflate(R.layout.fragment_new_plant_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getArgs()
        startPhotoIntent()
    }

    private fun getArgs() {
        arguments?.let {
            userId = it.getLong(userIdKey)
            plantType = it.getInt(plantTypeKey)
            location = it.getSerializable(locationKey) as Location?
            location?.let { gps.setLocation(it) }
            Lg.d("Fragment args: userId=$userId, plantType=$plantType, location=$location")
        }
    }

    private fun startPhotoIntent() {
        val capturePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        capturePhotoIntent.resolveActivity(activity.packageManager)

        try {
            val photoFile = createImageFile()
            val authorities = "${activity.packageName}.fileprovider"
            Lg.d("authorities = $authorities")
            val photoUri: Uri? = FileProvider.getUriForFile(activity, authorities, photoFile)
            Lg.d("photoUri = ${photoUri?.path}")
            capturePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(capturePhotoIntent, requestTakePhoto)
        } catch (e: IOException) {
            Toast.makeText(activity, "Photo not captured", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            requestTakePhoto -> {
                if (resultCode == Activity.RESULT_OK) {
                    Lg.d("New photo received")
//                    plantPhoto.setImageBitmap(getScaledBitmap())
                    if (oldPhotoUri.isNotEmpty()) {
                        Lg.d("Deleting old photo: $oldPhotoUri")
                        Lg.d("Delete photo result = ${File(oldPhotoUri).delete()}")
                        oldPhotoUri = ""
                    }
                    oldPhotoUri = photoUri


                    var bundle = bundleOf(
                            userIdKey to userId,
                            plantTypeKey to plantType,
                            photoUriKey to photoUri )
                    if (gps.gpsSwitch.isChecked)
                        bundle.putSerializable(locationKey, gps.currentLocation)
                    val navController = activity.findNavController(R.id.fragment)
                    navController.navigate(R.id.newPlantNameFragment, bundle)
                } else {
                    Toast.makeText(activity, "Photo not captured", Toast.LENGTH_SHORT).show()
                }
            }
            else -> Toast.makeText(activity, "Unrecognized request code", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val fileName: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val storageDir: File? = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        /storage/emulated/0/Android/data/com.geobotanica/files/Pictures
        val image: File = File.createTempFile(fileName, ".jpg", storageDir)
        photoUri = image.absolutePath
        return image
    }
}
