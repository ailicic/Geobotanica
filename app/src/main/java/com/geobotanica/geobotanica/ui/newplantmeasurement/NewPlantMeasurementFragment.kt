package com.geobotanica.geobotanica.ui.newplantmeasurement

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.*
import com.geobotanica.geobotanica.data.repo.MeasurementRepo
import com.geobotanica.geobotanica.data.repo.PhotoRepo
import com.geobotanica.geobotanica.data.repo.PlantLocationRepo
import com.geobotanica.geobotanica.data.repo.PlantRepo
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.util.Lg
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_new_plant_measurement.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*
import kotlinx.android.synthetic.main.measurement_compound_view.view.*
import javax.inject.Inject


// TODO: Handle back button better. Delete temp photo if needed, allow edit prev. activity, etc.

class NewPlantMeasurementFragment : BaseFragment() {
    @Inject lateinit var plantRepo: PlantRepo
    @Inject lateinit var plantLocationRepo: PlantLocationRepo
    @Inject lateinit var photoRepo: PhotoRepo
    @Inject lateinit var measurementRepo: MeasurementRepo

    override val className = this.javaClass.name.substringAfterLast('.')
    private var userId = 0L
    private lateinit var plantType: Plant.Type
    private lateinit var photoFilePath: String
    private var commonName: String? = null
    private var latinName: String? = null
    private var location: Location? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (getActivity() as BaseActivity).activityComponent.inject(this)
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
        return inflater.inflate(R.layout.fragment_new_plant_measurement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getArgs()

        heightMeasurement.textView.text = resources.getString(R.string.height)
        diameterMeasurement.textView.text = resources.getString(R.string.diameter)
        if (plantType == Plant.Type.TREE)
            trunkDiameterMeasurement.textView.text = resources.getString(R.string.trunk_diameter)
        else
            trunkDiameterMeasurement.visibility = View.GONE

        fab.setOnClickListener(::onFabPressed)
    }

    private fun getArgs() {
        arguments?.let {
            userId = it.getLong("userId")
            plantType = PlantTypeConverter.toPlantType( it.getInt("plantType") )
            photoFilePath = it.getString("photoFilePath")
            commonName = it.getString("commonName")
            latinName = it.getString("latinName")
            location = it.getSerializable("location") as Location?
            location?.let { gps.setLocation(it) }
            Lg.d("Fragment args: userId=$userId, plantType=$plantType, commonName=$commonName, " +
                    "latinName=$latinName, location=$location, photoFilePath=$photoFilePath")
        }
    }

    override fun onResume() {
        super.onResume()
        measurementsSwitch.setOnCheckedChangeListener(::onToggleAddMeasurement)
        measurementRadioGroup.setOnCheckedChangeListener(::onRadioButtonChecked)
        fab.setOnClickListener(::onFabPressed)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onToggleAddMeasurement(buttonView: CompoundButton, isChecked: Boolean) {
        Lg.d("onToggleHoldPosition(): isChecked=$isChecked")
        if (isChecked) {
            manualRadioButton.isEnabled = true
            assistedRadioButton.isEnabled = true
            heightMeasurement.visibility = View.VISIBLE
            diameterMeasurement.visibility = View.VISIBLE
            if (plantType == Plant.Type.TREE)
                trunkDiameterMeasurement.visibility = View.VISIBLE
        } else {
            manualRadioButton.isEnabled = false
            assistedRadioButton.isEnabled = false
            heightMeasurement.visibility = View.GONE
            diameterMeasurement.visibility = View.GONE
            trunkDiameterMeasurement.visibility = View.GONE
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onRadioButtonChecked(radioGroup: RadioGroup, checkedId: Int) {
        when (checkedId) {
            manualRadioButton.id -> {
                Lg.d("onRadioButtonChecked(): Manual")
                heightMeasurement.visibility = View.VISIBLE
                diameterMeasurement.visibility = View.VISIBLE

                if (plantType == Plant.Type.TREE)
                    trunkDiameterMeasurement.visibility = View.VISIBLE
            }
            assistedRadioButton.id -> {
                Lg.d("onRadioButtonChecked(): Assisted")
                heightMeasurement.visibility = View.GONE
                diameterMeasurement.visibility = View.GONE
                trunkDiameterMeasurement.visibility = View.GONE
            }
        }
    }

    // TODO: Push validation into the repo?
    private fun onFabPressed(view: View) {
        if (!gps.gpsSwitch.isEnabled) {
            Snackbar.make(view, "Wait for GPS fix", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }
        if (!gps.gpsSwitch.isChecked) {
            Snackbar.make(view, "Plant position must be held", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }
        if (measurementsSwitch.isChecked && isMeasurementEmpty() ) {
            Snackbar.make(view, "Provide plant measurements", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            return
        }
        Lg.d("Saving plant to database now...")
        val plant = Plant(userId, plantType, commonName, latinName)
        plant.id = plantRepo.insert(plant)
        Lg.d("Plant: $plant (id=${plant.id})")

        val photo = Photo(userId, plant.id, Photo.Type.COMPLETE, photoFilePath)
        photo.id = photoRepo.insert(photo)
        Lg.d("Photo: $photo (id=${photo.id})")

        gps.currentLocation?.let {
            val plantLocation = PlantLocation(plant.id, it)
            plantLocation.id = plantLocationRepo.insert(plantLocation)
            Lg.d("PlantLocation: $plantLocation (id=${plantLocation.id})")
        }

        if (measurementsSwitch.isChecked) {
            val height = heightMeasurement.getInCentimeters()
            val diameter = diameterMeasurement.getInCentimeters()
            val trunkDiameter = trunkDiameterMeasurement.getInCentimeters()
            measurementRepo.insert(Measurement(userId, plant.id, Measurement.Type.HEIGHT, height))
            measurementRepo.insert(Measurement(userId, plant.id, Measurement.Type.DIAMETER, diameter))
            if (trunkDiameter != 0F)
                measurementRepo.insert(Measurement(userId, plant.id, Measurement.Type.TRUNK_DIAMETER, trunkDiameter))
//            measurementRepo.getAllMeasurementsOfPlant(plant.id).forEachIndexed { i, measurement ->
//                Lg.d("#$i $measurement (id=${measurement.id})")
//            }
        }

        Toast.makeText(activity, "Plant saved", Toast.LENGTH_SHORT).show()

        val navController = activity.findNavController(R.id.fragment)
        navController.popBackStack(R.id.mapFragment, false)
    }

    private fun isMeasurementEmpty(): Boolean {
        return heightMeasurement.editText.text.isEmpty() ||
                diameterMeasurement.editText.text.isEmpty() ||
                (plantType == Plant.Type.TREE && trunkDiameterMeasurement.editText.text.isEmpty() )
    }
}
