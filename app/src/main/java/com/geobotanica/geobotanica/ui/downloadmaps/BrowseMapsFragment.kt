package com.geobotanica.geobotanica.ui.downloadmaps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.databinding.FragmentBrowseMapsBinding
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.dialog.WarningDialog
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_browse_maps.*
import javax.inject.Inject


class BrowseMapsFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<BrowseMapsViewModel>
    private lateinit var viewModel: BrowseMapsViewModel

    private val mapListAdapter = MapListAdapter(::onClickDownload, ::onClickCancel, ::onClickDelete, ::onClickFolder)
    private val parentMapFolderIds = mutableListOf<Long>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            Lg.d("BrowseMapsFragment bundle args: userId=$userId")
        }
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
        initRecyclerView()
        bindClickListeners()
        bindViewModel()
    }

    override fun onStart() {
        super.onStart()
        viewModel.syncDownloadStatuses()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainActivity.toolbar.setNavigationOnClickListener(null)
    }

    private fun addOnBackPressedCallback() {
        mainActivity.toolbar.setNavigationOnClickListener { onClickBackButton() }
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

    private fun initRecyclerView() {
        recyclerView.isVisible = true
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = mapListAdapter
        viewModel.mapListItems.observe(viewLifecycleOwner) { mapListAdapter.submitList(it) }
    }

    private fun bindClickListeners() = fab.setOnClickListener { navigateToNext() }

    private fun bindViewModel() {
        viewModel.mapListItems.observe(viewLifecycleOwner) { mapListAdapter.submitList(it) }
        viewModel.showMeteredNetworkDialog.observe(viewLifecycleOwner, onShowMeteredNetworkDialog)
        viewModel.showInsufficientStorageSnackbar.observe(viewLifecycleOwner, onShowInsufficientStorageSnackbar)
        viewModel.showInternetUnavailableSnackbar.observe(viewLifecycleOwner) { showSnackbar(resources.getString(R.string.internet_unavailable)) }
    }

    private val onShowMeteredNetworkDialog = Observer<Unit> {
        WarningDialog(
                getString(R.string.metered_network),
                getString(R.string.metered_network_confirm)
        ) {
            viewModel.onMeteredNetworkAllowed()
        }.show(parentFragmentManager, "tag")
    }

    private val onShowInsufficientStorageSnackbar = Observer<Unit> {
        showSnackbar(R.string.insufficient_storage, R.string.Inspect) {
            startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
        }
    }

    private fun onClickDownload(mapListItem: OnlineMapListItem) { viewModel.initDownload(mapListItem) }

    private fun onClickCancel(mapListItem: OnlineMapListItem) { viewModel.cancelDownloadWork(mapListItem.id) }

    private fun onClickDelete(mapListItem: OnlineMapListItem) {
        WarningDialog(
                getString(R.string.delete_map),
                getString(R.string.confirm_delete_map, mapListItem.printName)
        ) {
            viewModel.deleteMap(mapListItem.id)
        }.show(parentFragmentManager, null)
    }

    private fun onClickFolder(mapFolderItem: OnlineMapListItem) {
        Lg.d("onClickFolder(): ${mapFolderItem.printName}")
        parentMapFolderIds.add(mapFolderItem.id)
        viewModel.browseMapFolder(mapFolderItem.id)
    }

    private fun navigateToNext() {
        if (! popUpTo(R.id.mapFragment))
            navigateTo(R.id.action_browseMaps_to_map, createBundle())
    }

    private fun createBundle() = bundleOf(userIdKey to viewModel.userId)
}