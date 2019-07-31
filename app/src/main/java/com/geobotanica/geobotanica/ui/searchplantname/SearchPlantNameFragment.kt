package com.geobotanica.geobotanica.ui.searchplantname

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.PlantNameTag.*
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchFilterOptions
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.data_taxa.util.defaultFilterFlags
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.plantNameFilterOptionsKey
import com.geobotanica.geobotanica.util.*
import kotlinx.android.synthetic.main.fragment_search_plant_name.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import javax.inject.Inject

class SearchPlantNameFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<SearchPlantNameViewModel>
    private lateinit var viewModel: SearchPlantNameViewModel

    private lateinit var plantNameAdapter: PlantNameAdapter

    private var searchJob: Job? = null

    private val sharedPrefsFilterFlags = "filterFlags"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            photoUri = getFromBundle(photoUriKey)

            searchFilterOptions = SearchFilterOptions(sharedPrefs.get(sharedPrefsFilterFlags, defaultFilterFlags))
            Lg.d("Fragment args: userId=$userId, photoUri=$photoUri")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_search_plant_name, container, false)
    }

    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.taxonId = null
        viewModel.vernacularId = null
        viewModel.searchText = ""

        initRecyclerView()
        bindListeners()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.new_plant_name, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_filter -> {
            PlantNameFilterOptionsDialog().apply {
                arguments = bundleOf(
                        plantNameFilterOptionsKey to viewModel.searchFilterOptions.filterFlags)
                onApplyFilters = { filterOptions: SearchFilterOptions ->
                    sharedPrefs.put(sharedPrefsFilterFlags to filterOptions.filterFlags)
                    viewModel.searchFilterOptions = filterOptions
                    updateSearchResults()
                }

            }.show(this.fragmentManager!!,"tag")
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        searchJob?.cancel()
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    private fun initRecyclerView() {
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        plantNameAdapter = PlantNameAdapter(::onClickItem, ::onClickStar, true)
        recyclerView.adapter = plantNameAdapter
        updateSearchResults()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickItem(index: Int, result: SearchResult) {
        if (result.hasTag(COMMON))
            viewModel.vernacularId = result.id
        else if (result.hasTag(SCIENTIFIC))
            viewModel.taxonId = result.id
        if (result.hasTag(STARRED))
            viewModel.updateStarredTimestamp(result)
        navigateToNext()
    }

    private fun onClickStar(result: SearchResult) { viewModel.updateIsStarred(result) }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    private fun bindListeners() {
        searchEditText.onTextChanged(::onSearchEditTextChanged)
        clearButton.setOnClickListener { searchEditText.text.clear() }
        downloadButton.setOnClickListener(::onSkipPressed)
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

    @Suppress("UNUSED_PARAMETER")
    private fun onSkipPressed(view: View) {
        Lg.d("NewPlantFragment: onSkipPressed()")
        navigateToNext()
    }

    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    private fun updateSearchResults() {
        noResultsText.isVisible = false
        searchJob = lifecycleScope.launch {
            delay(300)
            progressBar.isVisible = true
            viewModel.searchPlantName(viewModel.searchText).consumeEach {
                plantNameAdapter.items = it
                plantNameAdapter.notifyDataSetChanged()
            }
        }
        searchJob?.invokeOnCompletion { completionError ->
            if (completionError != null) // Coroutine did not complete
                return@invokeOnCompletion
            progressBar.isVisible = false
            if (recyclerView.adapter?.itemCount == 0)
                noResultsText.isVisible = true
        }
    }

    private fun navigateToNext() {
        val navController = activity.findNavController(R.id.fragment)
        navController.navigate(R.id.newPlantNameFragment, createBundle())
    }

    private fun createBundle(): Bundle {
        return bundleOf(
                userIdKey to viewModel.userId,
                photoUriKey to viewModel.photoUri
        ).apply {
            viewModel.vernacularId?.let { putValue(vernacularIdKey, it) }
            viewModel.taxonId?.let { putValue(taxonIdKey, it) }
        }
    }
}