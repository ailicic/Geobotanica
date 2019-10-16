package com.geobotanica.geobotanica.ui.downloadmaps

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.databinding.FragmentBrowseMapsBinding
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.network.NetworkValidator.NetworkState.*
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.dialog.WarningDialog
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_browse_maps.*
import javax.inject.Inject


class BrowseMapsFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<BrowseMapsViewModel>
    private lateinit var viewModel: BrowseMapsViewModel

    @Inject lateinit var networkValidator: NetworkValidator

    private val mapListAdapter = MapListAdapter(::onClickDownload, ::onClickCancel, ::onClickDelete, ::onClickFolder)
    private val parentMapFolderIds = mutableListOf<Long>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentBrowseMapsBinding>(
                layoutInflater, R.layout.fragment_browse_maps, container, false).also {
            it.viewModel = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addOnBackPressedCallback()
        bindClickListeners()
        initRecyclerView()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.toolbar.setNavigationOnClickListener(null)
    }

    private fun addOnBackPressedCallback() {
        activity.toolbar.setNavigationOnClickListener { onClickBackButton() }
        requireActivity().onBackPressedDispatcher.addCallback(this) { onClickBackButton() }
    }

    private fun onClickBackButton() {
        if (parentMapFolderIds.isNotEmpty())
            browseParentFolder()
        else
            navigateBack()
    }

    private fun browseParentFolder() {
        parentMapFolderIds.removeAt(parentMapFolderIds.size - 1)
        if (parentMapFolderIds.isEmpty())
            viewModel.browseMapFolder(null)
        else
            viewModel.browseMapFolder(parentMapFolderIds.last())
    }

    private fun bindClickListeners() {
        fab.setOnClickListener { navigateToNext() }
    }

    private fun initRecyclerView() {
        recyclerView.isVisible = true
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = mapListAdapter
        viewModel.mapListItems.observe(viewLifecycleOwner, Observer {
            mapListAdapter.submitList(it)
        })
    }

    private fun onClickFolder(mapFolderItem: OnlineMapListItem) {
        Lg.d("onClickFolder(): ${mapFolderItem.printName}")
        parentMapFolderIds.add(mapFolderItem.id)
        viewModel.browseMapFolder(mapFolderItem.id)
    }


    @Suppress("UNUSED_PARAMETER")
    private fun onClickDownload(mapListItem: OnlineMapListItem) {
        when (networkValidator.getStatus()) {
            INVALID -> showSnackbar(resources.getString(R.string.internet_unavailable))
            VALID -> downloadMap(mapListItem)
            VALID_IF_METERED_PERMITTED -> {
                WarningDialog(
                        getString(R.string.metered_network),
                        getString(R.string.metered_network_confirm)
                ) {
                    networkValidator.allowMeteredNetwork(); downloadMap(mapListItem)
                }.show(requireFragmentManager(), "tag")
            }
        }
    }

    private fun downloadMap(mapListItem: OnlineMapListItem) { viewModel.downloadMap(mapListItem.id) }

    private fun onClickCancel(mapListItem: OnlineMapListItem) { viewModel.cancelDownload(mapListItem.status) }

    private fun onClickDelete(mapListItem: OnlineMapListItem) {
        WarningDialog(
                getString(R.string.delete_map),
                getString(R.string.confirm_delete_map, mapListItem.printName)
        ) {
            viewModel.deleteMap(mapListItem.id)
        }.show(requireFragmentManager(), null)
    }

    private fun navigateToNext() {
        if (defaultSharedPrefs.get(sharedPrefsIsFirstRunKey, true))
            navigateTo(R.id.action_browseMaps_to_map, popUpTo = R.id.browseMapsFragment)
        else
            popUpTo(R.id.mapFragment)
    }
}