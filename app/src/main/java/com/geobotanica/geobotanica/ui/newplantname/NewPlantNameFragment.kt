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
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.setScaledBitmap
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_new_plant_name.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*


class NewPlantNameFragment : BaseFragment() {
    override val className = this.javaClass.name.substringAfterLast('.')

    private var userId = 0L
    private var plantType = 0
    private var photoFilePath: String = ""
    private var location: Location? = null

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
        return inflater.inflate(R.layout.fragment_new_plant_name, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getArgs()
        plantPhoto.doOnPreDraw { plantPhoto.setScaledBitmap(photoFilePath) }
        fab.setOnClickListener(::onFabPressed)
    }

    private fun getArgs() {
        arguments?.let {
            userId = it.getLong("userId")
            plantType = it.getInt("plantType")
            photoFilePath = it.getString("photoFilePath")
            location = it.getSerializable("location") as Location?
            location?.let { gps.setLocation(it) }
            Lg.d("Fragment args: userId=$userId, plantType=$plantType, photoFilePath=$photoFilePath, location=$location")
        }
    }

    // TODO: Push validation into the repo?
    private fun onFabPressed(view: View) {
        Lg.d("NewPlantFragment: onSaveButtonPressed()")

        val commonName: String = commonNameEditText.editText!!.text.toString().trim()
        val latinName: String = latinNameEditText.editText!!.text.toString().trim()

        if (commonName.isEmpty() && latinName.isEmpty()) {
            Snackbar.make(view, "Provide a plant className", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }

        var bundle = bundleOf(
                "userId" to userId,
                "plantType" to plantType,
                "photoFilePath" to photoFilePath,
                "location" to location )
        if (commonName.isNotEmpty())
            bundle.putString("commonName", commonName)
        if (latinName.isNotEmpty())
            bundle.putString("latinName", latinName)
        if (gps.gpsSwitch.isChecked)
            bundle.putSerializable("location", gps.currentLocation)
        val navController = activity.findNavController(R.id.fragment)
        navController.navigate(R.id.newPlantMeasurementFragment, bundle)
    }
}