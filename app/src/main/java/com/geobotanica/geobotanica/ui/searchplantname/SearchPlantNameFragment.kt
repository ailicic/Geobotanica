package com.geobotanica.geobotanica.ui.searchplantname

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchFilterOptions
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.data_taxa.util.defaultFilterFlags
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.plantNameFilterOptionsKey
import com.geobotanica.geobotanica.ui.searchplantname.ViewEffect.*
import com.geobotanica.geobotanica.ui.searchplantname.ViewEvent.*
import com.geobotanica.geobotanica.util.*
import kotlinx.android.synthetic.main.fragment_search_plant_name.*
import javax.inject.Inject


class SearchPlantNameFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<SearchPlantNameViewModel>
    private lateinit var viewModel: SearchPlantNameViewModel

    private lateinit var plantNameAdapter: PlantNameAdapter

    private val sharedPrefsFilterFlags = "filterFlags"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_search_plant_name, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViewModel()
        viewModel.onEvent(ViewCreated(
                getFromBundle(userIdKey),
                getFromBundle(photoUriKey)
        ))
    }

    override fun onStart() {
        super.onStart()

        viewModel.onEvent(OnStart(searchEditText.toTrimmedString()))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_new_plant_name, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_filter -> {
            PlantNameFilterOptionsDialog().apply {
                arguments = bundleOf(
                        plantNameFilterOptionsKey to viewModel.viewState.value?.searchFilterOptions?.filterFlags)
                onApplyFilters = { filterOptions: SearchFilterOptions ->
                    viewModel.onEvent(SearchFilterSelected(filterOptions))
                }
            }.show(parentFragmentManager,"tag")
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun render(viewState: ViewState) {
        Lg.v("render()")
        noResultsText.isVisible = viewState.isNoResultsTextVisible
        loadingSpinner.isVisible = viewState.isLoadingSpinnerVisible
    }

    private fun execute(viewEffect: ViewEffect) {
        Lg.v("execute()")
        return when (viewEffect) {
            is InitView -> {
                initRecyclerView()
                loadSharedPrefs()
                bindListeners()
            }
            is UpdateSharedPrefs -> sharedPrefs.put(sharedPrefsFilterFlags to viewEffect.searchFilterOptions.filterFlags)
            is UpdateSearchResults -> {
                plantNameAdapter.showStars = viewEffect.showStars
                plantNameAdapter.items = viewEffect.searchResults
                plantNameAdapter.notifyDataSetChanged()
            }
            is ClearSearchText -> searchEditText.setText("")
            is NavigateToNext -> navigateToNext(viewEffect)
        }
    }

    private fun initRecyclerView() {
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        plantNameAdapter = PlantNameAdapter(
                onClickItem = { _, result: SearchResult -> viewModel.onEvent(ResultClicked(result)) },
                onClickStar = { result: SearchResult -> viewModel.onEvent(StarClicked(result)) },
                context= appContext
        )
        recyclerView.adapter = plantNameAdapter
    }

    private fun loadSharedPrefs() {
        val searchFilterOptions = SearchFilterOptions(sharedPrefs.get(sharedPrefsFilterFlags, defaultFilterFlags))
        viewModel.onEvent(SearchFilterSelected(searchFilterOptions))
    }

    private fun bindViewModel() {
        viewModel.viewState.observe(viewLifecycleOwner) { render(it) }
        viewModel.viewEffect.observe(viewLifecycleOwner) { execute(it) }
    }

    private fun bindListeners() {
        searchEditText.onTextChanged { viewModel.onEvent(SearchEditTextChanged(it)) }
        clearButton.setOnClickListener { viewModel.onEvent(ClearSearchClicked) }
        skipButton.setOnClickListener { viewModel.onEvent(SkipClicked) }
    }

    private fun navigateToNext(viewEffect: NavigateToNext) =
            navigateTo(R.id.action_searchPlantName_to_newPlantName, createBundle(viewEffect))

    private fun createBundle(viewEffect: NavigateToNext): Bundle {
        return bundleOf(
                userIdKey to viewEffect.userId,
                photoUriKey to viewEffect.photoUri,
                taxonIdKey to viewEffect.taxonId,
                vernacularIdKey to viewEffect.vernacularId
        )
    }
}