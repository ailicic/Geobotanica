package com.geobotanica.geobotanica.ui.newplantname

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data.entity.PlantTypeConverter
import com.geobotanica.geobotanica.data_taxa.TaxaDatabase
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchFilterOptions
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag.USED
import com.geobotanica.geobotanica.data_taxa.util.defaultPlantNameFilterFlags
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.plantNameFilterOptionsKey
import com.geobotanica.geobotanica.util.*
import kotlinx.android.synthetic.main.fragment_new_plant_name.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import javax.inject.Inject

class NewPlantNameFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantNameViewModel>
    private lateinit var viewModel: NewPlantNameViewModel

    private lateinit var plantNamesAdapter: PlantNamesAdapter

    private var searchJob: Job? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    private val sharedPrefsFilterFlags = "filterFlags"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            plantType = PlantTypeConverter.toPlantType(getFromBundle(plantTypeKey))
            photoUri = getFromBundle(photoUriKey)
            Lg.d("Fragment args: userId=$userId, plantType=$plantType, photoUri=$photoUri")

            searchFilterOptions = SearchFilterOptions(
                    sharedPrefs.get(sharedPrefsFilterFlags, defaultPlantNameFilterFlags)
            )
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_new_plant_name, container, false)
    }

    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
        bindListeners()
        TaxaDatabase.getInstance(appContext).close()
        searchEditText.requestFocus()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(com.geobotanica.geobotanica.R.menu.new_plant_name, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_filter -> {
            PlantNameFilterOptionsDialog().run {
                arguments = bundleOf(
                        plantNameFilterOptionsKey to viewModel.searchFilterOptions.filterFlags)
                onApplyFilters = { filterOptions: SearchFilterOptions ->
                    sharedPrefs.put(sharedPrefsFilterFlags to filterOptions.filterFlags)
                    viewModel.searchFilterOptions = filterOptions
                    updateSearchResults()
                }
                show(this@NewPlantNameFragment.fragmentManager,"tag")
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        searchJob?.cancel()
    }

    private fun initRecyclerView() = mainScope.launch {
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        plantNamesAdapter = PlantNamesAdapter(viewModel.getDefaultPlantNames(), onClickItem, onClickStar)
        recyclerView.adapter = plantNamesAdapter
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    private fun bindListeners() {
        fab.setOnClickListener(::onFabPressed)
        clearButton.setOnClickListener { searchEditText.text.clear() }
        searchEditText.onTextChanged(::onSearchEditTextChanged)
    }

    private val onClickItem = { result: SearchResult ->
        showToast(result.plantName)
        result.toggleTag(USED)
        viewModel.updateIsUsed(result)
    }

    private val onClickStar = { result: SearchResult ->
        viewModel.updateIsStarred(result)
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    private fun onSearchEditTextChanged(editText: String) {
        if (viewModel.searchText == editText)
            return
        searchJob?.cancel()
        viewModel.searchText = editText
        updateSearchResults()
    }

    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    private fun updateSearchResults() {
        noResultsText.isVisible = false
        searchJob = mainScope.launch {
            delay(300)
            if (viewModel.searchText.isBlank())
                showDefaultResults()
            else
                searchPlantName(viewModel.searchText)
        }
        searchJob?.invokeOnCompletion { completionError ->
            if (completionError == null) { // Coroutine completed normally
                progressBar.isVisible = false
                if (recyclerView.adapter?.itemCount == 0)
                    noResultsText.isVisible = true
            }
        }
    }

    private suspend fun showDefaultResults() {
        plantNamesAdapter.items = viewModel.getDefaultPlantNames()
        plantNamesAdapter.notifyDataSetChanged()
    }

    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    private suspend fun searchPlantName(string: String) {
        progressBar.isVisible = true
        viewModel.searchPlantName(string).consumeEach {
            plantNamesAdapter.items = it
            plantNamesAdapter.notifyDataSetChanged()
        }
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
        if (commonNameTextInput.isEmpty() && scientificNameTextInput.isEmpty()) {
            showSnackbar("Provide a plant name")
            return false
        }
        return true
    }

    private fun saveViewModelState() {
        commonNameTextInput.toString()
        val commonName = commonNameTextInput.toTrimmedString()
        val scientificName = scientificNameTextInput.toTrimmedString()
        viewModel.commonName = if (commonName.isNotEmpty()) commonName else null
        viewModel.scientificName = if (scientificName.isNotEmpty()) scientificName else null
    }

    private fun createBundle(): Bundle {
        return bundleOf(
                userIdKey to viewModel.userId,
                plantTypeKey to viewModel.plantType.ordinal,
                photoUriKey to viewModel.photoUri
        ).apply {
            viewModel.commonName?.let { putString(commonNameKey, it) }
            viewModel.scientificName?.let { putString(scientificNameKey, it) }
        }
    }
}