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
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.databinding.FragmentPlantDetailBinding
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.ImageViewExt.setScaledBitmap
import com.geobotanica.geobotanica.util.Lg
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_plant_detail.*
import javax.inject.Inject


class PlantDetailFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<PlantDetailViewModel>
    private lateinit var viewModel: PlantDetailViewModel

    private var plantId = 0L

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)
    }

    // TODO: Show plant type as icon (not shown anywhere yet)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        plantId = arguments?.getLong("plantId", 0L) ?: 0L
        Lg.d("Fragment args: plantId=$plantId")

        viewModel = getViewModel(viewModelFactory) {
            plantId = this@PlantDetailFragment.plantId
        }

        val binding = DataBindingUtil.inflate<FragmentPlantDetailBinding>(
                layoutInflater, R.layout.fragment_plant_detail, container, false).apply {
            viewModel = this@PlantDetailFragment.viewModel
            setLifecycleOwner(this@PlantDetailFragment)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPlantPhoto()
        bindClickListeners()
    }

    private fun setPlantPhoto() {
        plantPhoto.doOnPreDraw { _ ->
            viewModel.mainPhoto.observe(this, Observer {photo ->
                photo?.let {
                    plantPhoto.setScaledBitmap(it.fileName)
                }
            })
        }
    }

    private fun bindClickListeners() {
        deleteButton.setOnClickListener(::onDeleteButtonClicked)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Add new photos/measurements", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onDeleteButtonClicked(view: View) {
        AlertDialog.Builder(activity).apply {
            setTitle("Delete Plant")
            setMessage("Are you sure you want to delete this plant and its photos?")
            setPositiveButton("Yes") { _, _ ->
                viewModel.deletePlant()
                showToast("Plant deleted")

                val navController = activity.findNavController(R.id.fragment)
                navController.popBackStack()
            }
            setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            create()
        }.show()
    }
}