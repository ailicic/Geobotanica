package com.geobotanica.geobotanica.ui.plantdetail

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Photo
import com.geobotanica.geobotanica.databinding.FragmentPlantDetailBinding
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.setScaledBitmap
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_plant_detail.*
import javax.inject.Inject


class PlantDetailFragment : BaseFragment() {
    @Inject lateinit var plantDetailViewModelFactory: PlantDetailViewModelFactory
    private lateinit var viewModel: PlantDetailViewModel

    override val name = this.javaClass.name.substringAfterLast('.')

    private var plantId = 0L

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (getActivity() as BaseActivity).activityComponent.inject(this)
    }

    // TODO: Show plant type as icon (not shown anywhere yet)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        plantId = arguments?.getLong("plantId", 0L) ?: 0L
        Lg.d("Fragment args: plantId=$plantId")

        plantDetailViewModelFactory.plantId = plantId
        viewModel = ViewModelProviders.of(this, plantDetailViewModelFactory).get(PlantDetailViewModel::class.java)

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
        plantPhoto.doOnPreDraw {
            viewModel.mainPhoto.observe(this, Observer<Photo> {
                it?.let { photo ->
                    plantPhoto.setScaledBitmap(photo.fileName)
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
                Toast.makeText(context, "Plant deleted", Toast.LENGTH_SHORT).show()
                activity.finish()
            }
            setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            create()
        }.show()
    }
}
