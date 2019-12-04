package com.geobotanica.geobotanica.ui.newplantname

import android.content.Context
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.newplantname.ViewEffect.*
import com.geobotanica.geobotanica.ui.newplantname.ViewEvent.*
import com.geobotanica.geobotanica.ui.searchplantname.PlantNameAdapter
import com.geobotanica.geobotanica.util.*
import kotlinx.android.synthetic.main.fragment_new_plant_name.*
import kotlinx.coroutines.*
import javax.inject.Inject


class NewPlantNameFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantNameViewModel>
    private lateinit var viewModel: NewPlantNameViewModel

    private var plantNamesAdapter: PlantNameAdapter? = null

    private var commonNameListener: TextWatcher? = null
    private var scientificNameListener: TextWatcher? = null
    private var animateTextJob: Job? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            photoUri = getFromBundle(photoUriKey)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_plant_name, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViewModel()
        val vernacularId: Long? = getNullableFromBundle(vernacularIdKey)
        val taxonId: Long? = getNullableFromBundle(taxonIdKey)
        viewModel.onEvent(ViewCreated(vernacularId, taxonId))
        Lg.d("Fragment args: userId=${viewModel.userId}, vernId=$vernacularId, taxonId=$taxonId, photoUri=${viewModel.photoUri}")
    }

    private fun render(viewState: ViewState) {
        Lg.v("render()")
        commonNameEditText.isEnabled = viewState.isCommonNameEditable
        commonNameEditText.setText(viewState.commonName)
        commonNameEditText.setSelection(viewState.commonName.length)

        scientificNameEditText.isEnabled = viewState.isScientificNameEditable
        scientificNameEditText.setText(viewState.scientificName)
        scientificNameEditText.setSelection(viewState.scientificName.length)

        if (viewState.searchResults.isNotEmpty()) {
            suggestedText.isVisible = true
            suggestedText.text = viewState.suggestedText

            plantNamesAdapter?.run {
                if (viewState.isLastClickedShown != isLastSelectedShown || viewState.lastClickedResultIndex != lastSelectedIndex) {
                    isLastSelectedShown = viewState.isLastClickedShown
                    notifyItemChanged(lastSelectedIndex)
                    viewState.lastClickedResultIndex?.let { lastSelectedIndex = it }
                    notifyItemChanged(lastSelectedIndex)
                }
                if (viewState.searchResults.size > items.size) {
                    items = viewState.searchResults
                    notifyDataSetChanged()
                }
            }
        }
        gps.isVisible = viewState.searchResults.size <= 5
    }

    private fun execute(viewEffect: ViewEffect) {
        Lg.v("execute($viewEffect)")
        return when (viewEffect) {
            is InitView -> {
                initRecyclerView()
                bindListeners()
            }
            is ShowCommonNameAnimation -> showTypedNameAnimation(viewEffect.name, commonNameEditText)
            is ShowScientificNameAnimation -> showTypedNameAnimation(viewEffect.name, scientificNameEditText)
            is NavigateToNewPlantMeasurement -> {
                animateTextJob?.cancel()
                navigateTo(R.id.action_newPlantName_to_newPlantMeasurement, createBundle())
            }
            is NavigateToNewPlantType -> {
                animateTextJob?.cancel()
                navigateTo(R.id.action_newPlantName_to_newPlantType, createBundle())
            }
            is ShowPlantNameSnackbar -> showSnackbar("Provide a plant name")
        }
    }

    private fun bindViewModel() {
        viewModel.viewState.observe(viewLifecycleOwner) { render(it) }
        viewModel.viewEffect.observe(viewLifecycleOwner) { execute(it) }
    }

    private fun initRecyclerView() {
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        plantNamesAdapter = PlantNameAdapter(::onClickItem, ::onClickStar, appContext)
        recyclerView.adapter = plantNamesAdapter
    }

    private fun bindListeners() {
        bindTextListeners()
        fab.setOnClickListener {
            viewModel.onEvent(FabClicked(commonNameTextInput.toString(), scientificNameEditText.toString()))
        }
    }

    private fun bindTextListeners() {
        commonNameListener = commonNameEditText?.onTextChanged {
            if (viewModel.viewState.value?.commonName != it)
                viewModel.onEvent(CommonEditTextChanged(it))
        }
        scientificNameListener = scientificNameEditText?.onTextChanged {
            if (viewModel.viewState.value?.scientificName != it)
                viewModel.onEvent(ScientificEditTextChanged(it))
        }
    }

    private fun unbindTextListeners() {
        commonNameEditText?.removeTextChangedListener(commonNameListener)
        scientificNameEditText?.removeTextChangedListener(scientificNameListener)
    }

    private fun onClickItem(index: Int, result: SearchResult) = viewModel.onEvent(ResultClicked(index, result))

    private fun showTypedNameAnimation(string: String, editText: EditText) {
        animateTextJob?.cancel()
        unbindTextListeners()
        animateTextJob = lifecycleScope.launch {
            for (i in 1 .. string.length) {
                editText.setText(string.substring(0,i))
                editText.setSelection(i)
                delay(30)
            }
        }
        animateTextJob?.invokeOnCompletion { bindTextListeners() }
    }

    private fun onClickStar(result: SearchResult) = viewModel.onEvent(StarClicked(result))

    private fun createBundle(): Bundle {
        with (viewModel) {
            return bundleOf(
                    userIdKey to userId,
                    photoUriKey to photoUri,
                    commonNameKey to viewState.value?.commonName.nullIfBlank(),
                    scientificNameKey to viewState.value?.scientificName.nullIfBlank(),
                    vernacularIdKey to vernacularId,
                    taxonIdKey to taxonId,
                    plantTypeKey to plantTypes
            )
        }
    }
}