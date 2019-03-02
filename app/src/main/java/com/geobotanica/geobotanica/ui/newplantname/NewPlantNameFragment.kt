package com.geobotanica.geobotanica.ui.newplantname

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.PlantTypeConverter
import com.geobotanica.geobotanica.data_ro.PlantDatabaseRo
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.*
import kotlinx.android.synthetic.main.fragment_new_plant_name.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import javax.inject.Inject

// TODO: Show "No results" if list is empty
// TODO: Add filter for vern/sci name (action bar / menu button)
// TODO: Prioritize previously selected names in list (need history table)
// TODO: Add favourites button on each result and prioritize favourites
class NewPlantNameFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantNameViewModel>
    private lateinit var viewModel: NewPlantNameViewModel

    private lateinit var plantNamesRecyclerViewAdapter: PlantNamesRecyclerViewAdapter

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

    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
        bindViewListeners()

        GlobalScope.launch(Dispatchers.IO) {
            Lg.d("Count vern: ${viewModel.vernacularRepo.getCount()}")
            Lg.d("Count taxa: ${viewModel.taxonRepo.getCount()}")
        }
        PlantDatabaseRo.getInstance(appContext).close()
        searchEditText.requestFocus()
    }

    override fun onStop() {
        super.onStop()
        searchJob?.cancel()
    }

    private fun initRecyclerView() {
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        plantNamesRecyclerViewAdapter = PlantNamesRecyclerViewAdapter(emptyList(), onClickItem)
        recyclerView.adapter = plantNamesRecyclerViewAdapter
    }

    private var searchJob: Job? = null
    private var searchString = ""

    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    private fun bindViewListeners() {
        fab.setOnClickListener(::onFabPressed)

        searchEditText.onTextChanged { editText ->
//            Lg.d("New editText: $editText")
            if (searchString == editText)
                return@onTextChanged
            searchJob?.cancel()
            searchString = editText

            searchJob = GlobalScope.launch(Dispatchers.Main) {
                delay(300)
                if (searchString == editText) {
                    progressBar.isVisible = true
                    viewModel.searchPlantName(searchString).consumeEach {
                        plantNamesRecyclerViewAdapter.items = it
//                        Lg.d("Update: ${plantNamesRecyclerViewAdapter.items.size} hits for \"$searchString\"")
                        plantNamesRecyclerViewAdapter.notifyDataSetChanged()
                    }
                }
                progressBar.isVisible = false
            }
        }
    }

    private val onClickItem = { item: PlantNameSearchService.SearchResult ->
        showToast(item.name)
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