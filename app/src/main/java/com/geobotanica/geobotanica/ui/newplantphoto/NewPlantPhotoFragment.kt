package com.geobotanica.geobotanica.ui.newplantphoto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import java.io.File
import javax.inject.Inject


class NewPlantPhotoFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantPhotoViewModel>
    private lateinit var viewModel: NewPlantPhotoViewModel

    @Inject lateinit var storageHelper: StorageHelper

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            Lg.d("Fragment args: userId=$userId")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_plant_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deletePhotoIfExists()
        val photoFile = storageHelper.createPhotoFile(viewModel.userId)
        viewModel.photoUri = photoFile.absolutePath
        startPhotoIntent(photoFile)
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.currentLocation = null // Delete since exiting New Plant flow
    }

    private fun deletePhotoIfExists() {
        if (viewModel.photoUri.isNotEmpty()) {
            Lg.d("Deleting old photo: ${viewModel.photoUri}")
            Lg.d("Delete photo result = ${File(viewModel.photoUri).delete()}")
            viewModel.photoUri = ""
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            requestTakePhoto -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Lg.d("New photo received")
    //                    plantPhoto.setImageBitmap(getScaledBitmap())

                        val navController = NavHostFragment.findNavController(this)
                        navController.navigate(R.id.searchPlantNameFragment, createBundle())
                    }
                    Activity.RESULT_CANCELED -> { // "X" in GUI or back button pressed
                        Lg.d("onActivityResult: RESULT_CANCELED")
                        deletePhotoIfExists()
                        val navController = NavHostFragment.findNavController(this)
                        navController.popBackStack(R.id.newPlantTypeFragment, false)
                    }
                    else -> Lg.d("onActivityResult: Unrecognized code")
                }
            }
            else -> showToast("Unrecognized request code")
        }
    }

    private fun createBundle(): Bundle =
        bundleOf(
            userIdKey to viewModel.userId,
            photoUriKey to viewModel.photoUri)
}
