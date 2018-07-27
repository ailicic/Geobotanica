package com.geobotanica.geobotanica.ui.newplantname

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.ImageViewExt.setScaledBitmap
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.NavBundleExt.getFromBundle
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_new_plant_name.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*
import javax.inject.Inject


class NewPlantNameFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantNameViewModel>
    private lateinit var viewModel: NewPlantNameViewModel


    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            plantType = getFromBundle(plantTypeKey)
            photoUri = getFromBundle(photoUriKey)
            Lg.d("Fragment args: userId=$userId, plantType=$plantType, photoUri=$photoUri")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_plant_name, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setGpsLocationFromBundle()
        plantPhoto.doOnPreDraw { plantPhoto.setScaledBitmap(viewModel.photoUri) }
        fab.setOnClickListener(::onFabPressed)
    }

    private fun setGpsLocationFromBundle() =
        arguments?.getSerializable(locationKey)?.let { gps.setLocation(it as Location) }

    // TODO: Push validation into the repo?
    private fun onFabPressed(view: View) {
        Lg.d("NewPlantFragment: onSaveButtonPressed()")

        val commonName: String = commonNameEditText.editText!!.text.toString().trim()
        val latinName: String = latinNameEditText.editText!!.text.toString().trim()

        if (commonName.isEmpty() && latinName.isEmpty()) {
            Snackbar.make(view, "Provide a plant className", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .show()
            return
        }

        val bundle = bundleOf(
            userIdKey to viewModel.userId,
            plantTypeKey to viewModel.plantType,
            photoUriKey to viewModel.photoUri
        ).apply {
            if (commonName.isNotEmpty())
                putString(commonNameKey, commonName)
            if (latinName.isNotEmpty())
                putString(latinNameKey, latinName)
            if (gps.gpsSwitch.isChecked)
                putSerializable(locationKey, gps.currentLocation)
        }

        val navController = activity.findNavController(R.id.fragment)
        navController.navigate(R.id.newPlantMeasurementFragment, bundle)
    }
}