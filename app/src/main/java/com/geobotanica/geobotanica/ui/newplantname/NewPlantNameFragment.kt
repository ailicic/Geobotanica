package com.geobotanica.geobotanica.ui.newplantname

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.PlantTypeConverter
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.*
import kotlinx.android.synthetic.main.fragment_new_plant_name.*
import kotlinx.coroutines.*
import javax.inject.Inject


class NewPlantNameFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantNameViewModel>
    private lateinit var viewModel: NewPlantNameViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            plantType = PlantTypeConverter.toPlantType(getFromBundle(plantTypeKey))
            photoUri = getFromBundle(photoUriKey)
            Lg.d("Fragment args: userId=$userId, plantType=$plantType, photoUri=$photoUri")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_plant_name, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fab.setOnClickListener(::onFabPressed)

        // TODO: Need to "debounce" by ~300 ms
        // TODO: Show spinner while searching (maybe)
        searchEditText.onTextChanged {string ->
            if (string.length > 1) {
                GlobalScope.launch(Dispatchers.Main) {
                    resultsText.text = viewModel.searchPlantName(string)
                }
            }
        }

//        GlobalScope.launch(Dispatchers.IO) {
//            Lg.d("Count vern: ${vernacularDao.getCount()}")
//            Lg.d("Count taxa: ${taxonRepo.getCount()}")
//
//            val vernOrderedTime =measureTimeMillis {
//                val vernOrdered = vernacularRepo.getAllOrdered()
//                Lg.d("vernOrdered: $vernOrdered")
//            }
//            Lg.d("vernOrderedTime = $vernOrderedTime")
//        }

//        plantDatabaseRo.close()
    }

    // TODO: Push validation into the repo?
    @Suppress("UNUSED_PARAMETER")
    private fun onFabPressed(view: View) {
        Lg.d("NewPlantFragment: onSaveButtonPressed()")

        if (!areNamesValid())
            return
        saveViewModelState()

        val navController = activity.findNavController(R.id.fragment)
        navController.navigate(R.id.newPlantMeasurementFragment, createBundle())
    }

    private fun areNamesValid(): Boolean {
        if (commonNameTextInput.isEmpty() && latinNameTextInput.isEmpty()) {
            showSnackbar("Provide a plant name")
            return false
        }
        return true
    }

    private fun saveViewModelState() {
        commonNameTextInput.toString()
        val commonName = commonNameTextInput.toTrimmedString()
        val latinName = latinNameTextInput.toTrimmedString()
        viewModel.commonName = if (commonName.isNotEmpty()) commonName else null
        viewModel.latinName = if (latinName.isNotEmpty()) latinName else null
    }

    private fun createBundle(): Bundle {
        return bundleOf(
                userIdKey to viewModel.userId,
                plantTypeKey to viewModel.plantType.ordinal,
                photoUriKey to viewModel.photoUri
        ).apply {
            viewModel.commonName?.let { putString(commonNameKey, it) }
            viewModel.latinName?.let { putString(latinNameKey, it) }
        }
    }
}