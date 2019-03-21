package com.geobotanica.geobotanica.ui.newplanttype

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.data.entity.Plant.Type.Companion.allTypeFlags
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import com.geobotanica.geobotanica.util.getNullableFromBundle
import com.geobotanica.geobotanica.util.putValue
import kotlinx.android.synthetic.main.fragment_new_plant_type.*
import javax.inject.Inject


class NewPlantTypeFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantTypeViewModel>
    private lateinit var viewModel: NewPlantTypeViewModel

    private lateinit var plantTypeAdapter: PlantTypeAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            photoUri = getFromBundle(photoUriKey)
            commonName = getNullableFromBundle(commonNameKey)
            scientificName = getNullableFromBundle(scientificNameKey)
            vernacularId = getNullableFromBundle(vernacularIdKey)
            taxonId = getNullableFromBundle(taxonIdKey)
            plantTypeOptions = Plant.Type.flagsToList(getFromBundle(plantTypeKey, allTypeFlags))
            Lg.d("Fragment args: plantTypeOptions=$plantTypeOptions, userId=$userId, commonName=$commonName, " +
                    "scientificName=$scientificName, vernId=$vernacularId, taxonId=$taxonId, photoUri=$photoUri")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_plant_type, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
    }

    private fun initRecyclerView() {
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        plantTypeAdapter = PlantTypeAdapter(viewModel.plantTypeOptions, ::onClickItem)
        recyclerView.adapter = plantTypeAdapter
    }

    private fun onClickItem(plantType: Plant.Type) {
        viewModel.plantType = plantType
        navigateToNext()
    }

    private fun navigateToNext() {
        val navController = activity.findNavController(R.id.fragment)
        navController.navigate(R.id.newPlantMeasurementFragment, createBundle())
    }

    private fun createBundle(): Bundle =
        bundleOf(
            userIdKey to viewModel.userId,
            photoUriKey to viewModel.photoUri,
            plantTypeKey to viewModel.plantType.flag
        ).apply {
            viewModel.commonName?.let { putValue(commonNameKey, it) }
            viewModel.scientificName?.let { putValue(scientificNameKey, it) }
            viewModel.vernacularId?.let { putValue(vernacularIdKey, it) }
            viewModel.taxonId?.let { putValue(taxonIdKey, it) }
        }
}
