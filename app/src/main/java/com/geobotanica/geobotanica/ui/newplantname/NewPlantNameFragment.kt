package com.geobotanica.geobotanica.ui.newplantname

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.data_taxa.util.PlantNameSearchService.SearchResult
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.searchplantname.PlantNameAdapter
import com.geobotanica.geobotanica.util.*
import kotlinx.android.synthetic.main.fragment_new_plant_name.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import javax.inject.Inject


class NewPlantNameFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<NewPlantNameViewModel>
    private lateinit var viewModel: NewPlantNameViewModel

    private lateinit var plantNamesAdapter: PlantNameAdapter

    private var loadNamesJob: Job? = null
    private var animateTextJob: Job = Job().apply { cancel() } // Ensure job is not active (needed non-null for simplicity)

    private var activeEditText: EditText? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        initViewModel()
    }

    private fun initViewModel() {
        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            photoUri = getFromBundle(photoUriKey)
            vernacularId = getNullableFromBundle(vernacularIdKey)
            taxonId = getNullableFromBundle(taxonIdKey)
            lastSelectedIndex = null
            lastSelectedId = null
            lastSelectedName = ""

            Lg.d("Fragment args: userId=$userId, vernId=$vernacularId, taxonId=$taxonId, photoUri=$photoUri")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_new_plant_name, container, false)
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.taxonId == null && viewModel.vernacularId == null)
            gps.isVisible = true

        initRecyclerView()
        loadPlantNames()
        initEditTexts()
        bindListeners()
    }

    override fun onStop() {
        super.onStop()
        loadNamesJob?.cancel()
        animateTextJob.cancel()
    }

    private fun initRecyclerView() {
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        plantNamesAdapter = PlantNameAdapter(::onClickItem, ::onClickStar, true)
        viewModel.lastSelectedIndex?. let { plantNamesAdapter.selectedIndex = it }
        recyclerView.adapter = plantNamesAdapter
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    private fun loadPlantNames() {
        with (viewModel) {
            loadNamesJob = lifecycleScope.launch {
                loadNamesFromIds()

                val suggestedNamesChannel: ReceiveChannel<List<SearchResult>>? =
                        when {
                            vernacularId != null -> searchSuggestedScientificNames(vernacularId!!)
                            taxonId != null -> searchSuggestedCommonNames(taxonId!!)
                            else -> null
                        }

                suggestedNamesChannel?.consumeEach { results ->
                    plantNamesAdapter.items = results
                    plantNamesAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun initEditTexts() {
        with (viewModel) {
            taxonId = getNullableFromBundle(taxonIdKey) // Need to reset if returning to activity with back button
            vernacularId = getNullableFromBundle(vernacularIdKey)

            viewModel.vernacularId?.let {
                commonNameEditText.isEnabled = false
                activeEditText = scientificNameEditText
                suggestedText.text = resources.getString(R.string.suggested_scientific)
                suggestedText.isVisible = true
            }
            viewModel.taxonId?.let {
                scientificNameEditText.isEnabled = false
                activeEditText = commonNameEditText
                suggestedText.text = resources.getString(R.string.suggested_common)
                suggestedText.isVisible = true
            }
            loadNamesJob?.invokeOnCompletion { completionError ->
                if (completionError != null) // Coroutine did not complete
                    return@invokeOnCompletion
                commonName?.let {
                    if (commonNameEditText.text.toString() != it)
                        showTypedNameAnimation(it, commonNameEditText)
                }
                scientificName?.let {
                    if (scientificNameEditText.text.toString() != it)
                        showTypedNameAnimation(it, scientificNameEditText)
                }
            }
        }
    }

    private fun onClickItem(index: Int, result: SearchResult) {
        with (viewModel) {
            lastSelectedName = result.plantName
            if (index == lastSelectedIndex) {
                activeEditText?.setText(lastSelectedName)
                activeEditText?.setSelection(lastSelectedName.length)
            } else
                showTypedNameAnimation(lastSelectedName, activeEditText!!)

            lastSelectedId = result.id
            lastSelectedIndex = index
        }
    }

    private fun showTypedNameAnimation(string: String, editText: EditText) {
        animateTextJob.cancel()
        animateTextJob = lifecycleScope.launch {
            for (i in 1 .. string.length) {
                editText.setText(string.substring(0,i))
                editText.setSelection(i)
                delay(30)
            }
        }
        animateTextJob.invokeOnCompletion { completionError ->
            if (completionError != null)
                editText.setText(string)
        }
    }

    private fun onClickStar(result: SearchResult) { viewModel.updateIsStarred(result) }

    private fun bindListeners() {
        activeEditText?.onTextChanged(::onSearchEditTextChanged)
        fab.setOnClickListener(::onClickFab)
    }

    private fun onSearchEditTextChanged(editText: String) {
        if (!animateTextJob.isActive) {
            val isSelectable = editText == viewModel.lastSelectedName
            if (plantNamesAdapter.isSelectable != isSelectable) {
                plantNamesAdapter.isSelectable = isSelectable
                viewModel.lastSelectedIndex?.let { plantNamesAdapter.notifyItemChanged(it) }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickFab(view: View) {
        Lg.d("NewPlantFragment: onClickFab()")

       if (!areNamesValid())
            return
        saveViewModelState()
        navigateToNext()
    }

    private fun navigateToNext() {
        animateTextJob.cancel()
        val navController = activity.findNavController(R.id.fragment)

        if (viewModel.isPlantTypeKnown())
            navController.navigate(R.id.newPlantMeasurementFragment, createBundle())
        else
            navController.navigate(R.id.newPlantTypeFragment, createBundle())
    }

    private fun areNamesValid(): Boolean {
        if (commonNameTextInput.isEmpty() && scientificNameTextInput.isEmpty()) {
            showSnackbar("Provide a plant name")
            return false
        }
        return true
    }

    private fun saveViewModelState() {
        val commonNameText = commonNameEditText.toTrimmedString()
        val scientificNameText = scientificNameEditText.toTrimmedString()
        Lg.d("commonNameText=$commonNameText, scientificNameText=$scientificNameText")
        with (viewModel) {
            commonName = if (commonNameText.isEmpty()) null else commonNameText
            scientificName = if (scientificNameText.isEmpty()) null else scientificNameText

            if (activeEditText?.text.toString() == lastSelectedName) {
                if (taxonId == null)
                    taxonId = lastSelectedId
                else if (vernacularId == null)
                    vernacularId = lastSelectedId
            } else {
                lastSelectedIndex = null
                lastSelectedId = null
            }
            getPlantTypes()
        }
    }

    private fun createBundle(): Bundle {
        with (viewModel) {
            return bundleOf(
                    userIdKey to userId,
                    photoUriKey to photoUri
            ).apply {
                commonName?.let { putValue(commonNameKey, it) }
                scientificName?.let { putValue(scientificNameKey, it) }
                taxonId?.let { putValue(taxonIdKey, it) }
                vernacularId?.let { putValue(vernacularIdKey, it) }
                plantTypes?.let { putValue(plantTypeKey, it) }
            }
        }
    }
}