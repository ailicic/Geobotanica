package com.geobotanica.geobotanica.ui.newplantname

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.addmeasurement.AddMeasurementsActivity
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.setScaledBitmap
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_new_plant_name.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*


class NewPlantNameFragment : BaseFragment() {

    override val name = this.javaClass.name.substringAfterLast('.')

    private var userId = 0L
    private var plantType = 0
    private var photoFilePath: String = ""
    private var plantLocation: Location? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (getActivity() as BaseActivity).activityComponent.inject(this)

        getArgs()
    }

    private fun getArgs() {
        arguments?.let {
            userId = it.getLong("userId")
            plantType = it.getInt("plantType")
            photoFilePath = it.getString("photoFilePath")
            plantLocation = it.getSerializable("plantLocation") as Location?
            plantLocation?.let { gps.setLocation(it) }
            Lg.d("Fragment args: userId=$userId, plantType=$plantType, photoFilePath=$photoFilePath, plantLocation=$plantLocation")
        }
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
        plantPhoto.doOnPreDraw { plantPhoto.setScaledBitmap(photoFilePath) }
        fab.setOnClickListener(::onFabPressed)
    }

    // TODO: Push validation into the repo?
    private fun onFabPressed(view: View) {
        Lg.d("NewPlantFragment: onSaveButtonPressed()")

        val commonName: String = commonNameEditText.editText!!.text.toString().trim()
        val latinName: String = latinNameEditText.editText!!.text.toString().trim()

        if (commonName.isEmpty() && latinName.isEmpty()) {
            Snackbar.make(view, "Provide a plant name", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }

        val intent = Intent(activity, AddMeasurementsActivity::class.java)
                .putExtra(getString(R.string.extra_user_id), userId)
                .putExtra(getString(R.string.extra_plant_type), plantType)
                .putExtra(getString(R.string.extra_plant_photo_path), photoFilePath)
        if (commonName.isNotEmpty())
            intent.putExtra(getString(R.string.extra_plant_common_name), commonName)
        if (latinName.isNotEmpty())
            intent.putExtra(getString(R.string.extra_plant_latin_name), latinName)
        if (gps.gpsSwitch.isChecked)
            intent.putExtra(getString(R.string.extra_location), gps.currentLocation)
        startActivity(intent)
    }

}
