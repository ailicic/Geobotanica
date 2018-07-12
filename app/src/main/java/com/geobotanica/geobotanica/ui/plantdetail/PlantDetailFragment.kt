package com.geobotanica.geobotanica.ui.plantdetail

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.Photo
import com.geobotanica.geobotanica.databinding.FragmentPlantDetailBinding
import com.geobotanica.geobotanica.ui.BaseActivity
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.setScaledBitmap
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

        plantId = activity.intent.getLongExtra(getString(R.string.extra_plant_id), -1)
        Lg.d("Intent extras: plantId=$plantId")

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

    // TODO: Find a better way to load the photo
    private fun setPlantPhoto() {
        val viewTreeObserver = plantPhoto.viewTreeObserver
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    @Suppress("DEPRECATION")
                    plantPhoto.viewTreeObserver.removeGlobalOnLayoutListener(this)
                    viewModel.mainPhoto.observe(this@PlantDetailFragment, Observer<Photo> {
                        it?.let {photo ->
                            plantPhoto.setScaledBitmap(photo.fileName)
                        }
                    })
                }
            })
        }
    }

    private fun bindClickListeners() {
        deleteButton.setOnClickListener(::onDeleteButtonClicked)
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
