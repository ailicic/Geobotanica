package com.geobotanica.geobotanica.ui.newplanttype

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantType
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
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
        }
        Lg.d("Fragment args: userId=${viewModel.userId}")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_plant_type, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.currentLocation = null // Delete since exiting New Plant flow
    }

    private fun initRecyclerView() {
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        plantTypeAdapter = PlantTypeAdapter(PlantType.values().toList(), ::onClickItem)
        recyclerView.adapter = plantTypeAdapter
    }

    private fun onClickItem(plantType: PlantType) {
        showToast("${plantType.ordinal}")
//        navigateToNext()
    }

//    private fun createBundle(plantType: Plant.Type): Bundle =
//        bundleOf(
//            userIdKey to viewModel.userId,
//            plantTypeKey to plantType.ordinal)

}
