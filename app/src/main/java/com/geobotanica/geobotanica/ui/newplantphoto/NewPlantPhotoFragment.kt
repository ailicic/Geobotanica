package com.geobotanica.geobotanica.ui.newplantphoto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
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
            currentSessionId = getFromBundle(newPlantSessionIdKey)
            Lg.d("Fragment args: userId=$userId")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_plant_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.deleteLastPhoto()
        val photoFile = storageHelper.createPhotoFile()
        viewModel.photoUri = photoFile.absolutePath
        startPhotoIntent(photoFile)
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.currentLocation = null // Delete since exiting New Plant flow
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            requestTakePhoto -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Lg.d("New photo received")
    //                    plantPhoto.setImageBitmap(getScaledBitmap())
                        navigateTo(R.id.action_newPlantPhoto_to_searchPlantName, createBundle())
                    }
                    Activity.RESULT_CANCELED -> { // "X" in GUI or back button pressed
                        Lg.d("onActivityResult: RESULT_CANCELED")
                        viewModel.deleteLastPhoto()
                        navigateBack()
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
