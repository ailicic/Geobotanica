package com.geobotanica.geobotanica.ui.plantdetail

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.databinding.FragmentPlantDetailBinding
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.setScaledBitmap
import kotlinx.android.synthetic.main.fragment_plant_detail.*
import javax.inject.Inject

// TODO: Double check how measurements are handled here. Integrate changes to Measurement and MeasurementEditView
// TODO: Show plant type as icon (not shown anywhere yet)

class PlantDetailFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<PlantDetailViewModel>
    private lateinit var viewModel: PlantDetailViewModel

    private var plantId = 0L

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        plantId = arguments?.getLong("plantId", 0L) ?: 0L
        Lg.d("Fragment args: plantId=$plantId")

        viewModel = getViewModel(viewModelFactory) {
            plantId = this@PlantDetailFragment.plantId
        }

        val binding = DataBindingUtil.inflate<FragmentPlantDetailBinding>(
                layoutInflater, R.layout.fragment_plant_detail, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPlantPhoto()
        bindClickListeners()
    }

    private fun setPlantPhoto() {
        plantPhoto.doOnPreDraw {
            viewModel.mainPhoto.observe(viewLifecycleOwner, Observer { mainPhoto ->
                mainPhoto?.let { photo ->
                    plantPhoto.setScaledBitmap(viewModel.getPhotoUri(photo))
                }
            })
        }
    }

    private fun bindClickListeners() {
        deleteButton.setOnClickListener(::onClickDelete)
        fab.setOnClickListener { showSnackbar("Add new photos/measurements") }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickDelete(view: View) {
        AlertDialog.Builder(activity).apply {
            setTitle(getString(R.string.delete_plant))
            setMessage(getString(R.string.delete_plant_confirm))
            setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.deletePlant()
                showToast(getString(R.string.plant_deleted))

                val navController = NavHostFragment.findNavController(this@PlantDetailFragment)
                navController.popBackStack()
            }
            setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.dismiss() }
            create()
        }.show()
    }
}