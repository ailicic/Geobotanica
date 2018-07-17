package com.geobotanica.geobotanica.ui.newplanttype

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Plant
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.util.Lg
import kotlinx.android.synthetic.main.fragment_new_plant_type.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*


class NewPlantTypeFragment : BaseFragment() {

    override val className = this.javaClass.name.substringAfterLast('.')

    private var userId = 0L

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (getActivity() as BaseActivity).activityComponent.inject(this)

        userId = arguments?.getLong("userId") ?: 0L
        Lg.d("Fragment args: userId=$userId")
    }

    // TODO: Show plant type as icon (not shown anywhere yet)
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
        return inflater.inflate(R.layout.fragment_new_plant_type, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindClickListeners()
    }

    private fun bindClickListeners() {
        buttonTree.setOnClickListener(::onClickListener)
        buttonShrub.setOnClickListener(::onClickListener)
        buttonHerb.setOnClickListener(::onClickListener)
        buttonGrass.setOnClickListener(::onClickListener)
        buttonVine.setOnClickListener(::onClickListener)
    }

    private fun onClickListener(view: View) {
        var plantType = Plant.Type.TREE
        when(view) {
            buttonTree -> plantType = Plant.Type.TREE
            buttonShrub -> plantType = Plant.Type.SHRUB
            buttonHerb -> plantType = Plant.Type.HERB
            buttonGrass -> plantType = Plant.Type.GRASS
            buttonVine -> plantType = Plant.Type.VINE
        }
        Lg.d("onClickListener(): Clicked $plantType")

        var bundle = bundleOf(
                "userId" to userId,
                "plantType" to plantType.ordinal)
        if (gps.gpsSwitch.isChecked)
            bundle.putSerializable("location", gps.currentLocation)
        val navController = activity.findNavController(R.id.fragment)
        navController.navigate(R.id.newPlantPhotoFragment, bundle)
    }

}
