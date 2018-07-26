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
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.NavBundleExt.getFromBundleOrPrefs
import com.geobotanica.geobotanica.util.SharedPrefsExt.putSharedPrefs
import kotlinx.android.synthetic.main.fragment_new_plant_type.*
import kotlinx.android.synthetic.main.gps_compound_view.view.*
import javax.inject.Inject


class NewPlantTypeFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantTypeViewModel>
    private lateinit var viewModel: NewPlantTypeViewModel

    // SharedPrefs
    private val newPlantTypeSharedPrefs = "newPlantTypeSharedPrefs"
    override val sharedPrefsKey = "userId"

    override val className = this.javaClass.name.substringAfterLast('.')

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundleOrPrefs(userIdKey, 0L)
        }
        Lg.d("Fragment args: userId=${viewModel.userId}")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_plant_type, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindClickListeners()
    }

    override fun onStop() {
        super.onStop()
        putSharedPrefs(newPlantTypeSharedPrefs, userIdKey to viewModel.userId)
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
            userIdKey to viewModel.userId,
            plantTypeKey to plantType.ordinal)
        if (gps.gpsSwitch.isChecked)
            bundle.putSerializable(locationKey, gps.currentLocation)
        val navController = activity.findNavController(R.id.fragment)
        navController.navigate(R.id.newPlantPhotoFragment, bundle)
    }

}
