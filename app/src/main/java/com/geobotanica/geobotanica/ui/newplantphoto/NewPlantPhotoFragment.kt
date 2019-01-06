package com.geobotanica.geobotanica.ui.newplantphoto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data.entity.PlantTypeConverter
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import kotlinx.android.synthetic.main.fragment_new_plant_photo.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*
import java.io.File
import javax.inject.Inject


class NewPlantPhotoFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantPhotoViewModel>
    private lateinit var viewModel: NewPlantPhotoViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            plantType = PlantTypeConverter.toPlantType(getFromBundle(plantTypeKey))
            oldPhotoUri = ""
            Lg.d("Fragment args: userId=$userId, plantType=$plantType")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_plant_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setGpsLocationFromBundle()
        val photoFile = createPhotoFile()
        viewModel.photoUri = photoFile.absolutePath
        startPhotoIntent(photoFile)
    }

    private fun setGpsLocationFromBundle() =
        arguments?.getSerializable(locationKey)?.let { gps.setLocation(it as Location) }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            requestTakePhoto -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Lg.d("New photo received")
        //                    plantPhoto.setImageBitmap(getScaledBitmap())
                        if (viewModel.oldPhotoUri.isNotEmpty()) {
                            Lg.d("Deleting old photo: ${viewModel.oldPhotoUri}")
                            Lg.d("Delete photo result = ${File(viewModel.oldPhotoUri).delete()}")
                            viewModel.oldPhotoUri = ""
                        }
                        viewModel.oldPhotoUri = viewModel.photoUri

                        val navController = activity.findNavController(R.id.fragment)
                        navController.navigate(R.id.newPlantNameFragment, createBundle())
                    }
                    Activity.RESULT_CANCELED -> {
                        Lg.d("onActivityResult: RESULT_CANCELED") // "X" in GUI or back button pressed

                        val navController = activity.findNavController(R.id.fragment)
                        navController.popBackStack(R.id.newPlantTypeFragment, false)
                    }
                    else -> Lg.d("onActivityResult: Unrecognized code")
                }
            }
            else -> Toast.makeText(activity, "Unrecognized request code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createBundle(): Bundle {
        val bundle = bundleOf(
                userIdKey to viewModel.userId,
                plantTypeKey to viewModel.plantType.ordinal,
                photoUriKey to viewModel.photoUri)
        if (gps.gpsSwitch.isChecked)
            bundle.putSerializable(locationKey, gps.currentLocation)
        return bundle
    }
}
