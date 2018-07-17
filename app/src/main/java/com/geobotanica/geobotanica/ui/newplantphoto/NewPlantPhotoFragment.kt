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
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.fragment_new_plant_type.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class NewPlantPhotoFragment : BaseFragment() {

    override val name = this.javaClass.name.substringAfterLast('.')

    private var userId = 0L
    private var plantType = 0
    private var plantLocation: Location? = null
    private val requestTakePhoto = 2
    private var photoFilePath: String = ""
    private var oldPhotoFilePath: String = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (getActivity() as BaseActivity).activityComponent.inject(this)

        getArgs()
        capturePhoto()
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

    private fun getArgs() {
        arguments?.let {
            userId = it.getLong("userId")
            plantType = it.getInt("plantType")
            plantLocation = it.getSerializable("plantLocation") as Location?
            plantLocation?.let { gps.setLocation(it) }
            Lg.d("Fragment args: userId=$userId, plantType=$plantType, location=$plantLocation")
        }
    }

    private fun capturePhoto() {
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
                    if (oldPhotoFilePath.isNotEmpty()) {
                        Lg.d("Deleting old photo: $oldPhotoFilePath")
                        Lg.d("Delete photo result = ${File(oldPhotoFilePath).delete()}")
                        oldPhotoFilePath = ""
                    }
                    oldPhotoFilePath = photoFilePath


                    var bundle = bundleOf(
                            "userId" to userId,
                            "plantType" to plantType,
                            "photoFilePath" to photoFilePath,
                            "plantLocation" to plantLocation )
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
        photoFilePath = image.absolutePath
        return image
    }

}
