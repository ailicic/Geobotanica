package com.geobotanica.geobotanica.ui.downloadmaps

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.databinding.FragmentLocalMapsBinding
import com.geobotanica.geobotanica.network.Resource
import com.geobotanica.geobotanica.network.ResourceStatus.*
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.dialog.WarningDialog
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.get
import com.geobotanica.geobotanica.util.getFromBundle
import com.geobotanica.geobotanica.util.put
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_local_maps.*
import javax.inject.Inject


class LocalMapsFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<LocalMapsViewModel>
    private lateinit var viewModel: LocalMapsViewModel

    private val mapListAdapter = MapListAdapter(::onClickDownload, ::onClickCancel, ::onClickDelete)
    private val sharedPrefsExitOnBack = "exitOnBackPermitted"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentLocalMapsBinding>(
                layoutInflater, R.layout.fragment_local_maps, container, false).also {
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
        viewModel.getMapsFromExtStorage()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.toolbar.setNavigationOnClickListener(null)
    }

    private fun addOnBackPressedCallback() {
        if (defaultSharedPrefs.get(sharedPrefsIsFirstRunKey, true)) {
            activity.toolbar.setNavigationOnClickListener { exitAppWithWarning() }
            requireActivity().onBackPressedDispatcher.addCallback(this) { exitAppWithWarning() }
        }
    }

    private fun exitAppWithWarning() {
        Lg.d("LocalMapsFragment: exitAppWithWarning()")
        val isPermitted = sharedPrefs.get(sharedPrefsExitOnBack, false)
        if (isPermitted)
            activity.finish()
        else {
            WarningDialog(
                    getString(R.string.exit_app),
                    getString(R.string.exit_app_confirm)
            ) {
                sharedPrefs.put(sharedPrefsExitOnBack to true)
                activity.finish()
            }.show((activity as FragmentActivity).supportFragmentManager, "tag")
        }
    }

    private fun initRecyclerView() {
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = mapListAdapter
    }

    private fun bindClickListeners() {
        browseMapsButton.setOnClickListener { browseMaps() }
        getMapsButton.setOnClickListener { rebindViewModel() }
        fab.setOnClickListener { navigateToNext() }
    }

    private fun bindViewModel() {
        viewModel.localMaps.observe(viewLifecycleOwner, onLocalMaps)
        viewModel.showMeteredNetworkDialog.observe(viewLifecycleOwner, onShowMeteredNetworkDialog)
        viewModel.showInternetUnavailableSnackbar.observe(viewLifecycleOwner, Observer {
            showSnackbar(resources.getString(R.string.internet_unavailable))
        })
    }

    private fun rebindViewModel() {
        viewModel.localMaps.removeObserver(onLocalMaps)
        bindViewModel()
    }

    private val onLocalMaps = Observer<Resource<List<OnlineMapListItem>>> { mapListItems ->
//        Lg.v("LocalMapsFragment: onLocalMaps: $mapListItems")
        when (mapListItems.status) {
            LOADING -> {
                searchingOnlineMapsText.isVisible = true
                noResultsText.isVisible = false
                progressBar.isVisible = true
                getMapsButton.isVisible = false
                browseMapsButton.isVisible = false
                recyclerView.isVisible = true
                mapListAdapter.submitList(mapListItems.data)
            }
            SUCCESS -> {
                searchingOnlineMapsText.isVisible = false
                progressBar.isVisible = false
                getMapsButton.isVisible = false
                browseMapsButton.isVisible = true

                if (mapListItems.data.isNullOrEmpty()) {
                    noResultsText.isVisible = true
                } else {
                    recyclerView.isVisible = true
                    mapListAdapter.submitList(mapListItems.data)
                }
            }
            ERROR -> {
                searchingOnlineMapsText.isVisible = false
                noResultsText.isVisible = false
                progressBar.isVisible = false
                getMapsButton.isVisible = true
                browseMapsButton.isVisible = true

                showSnackbar(R.string.geolocation_error)
            }
        }
    }

    private val onShowMeteredNetworkDialog = Observer<Unit> {
        WarningDialog(
                getString(R.string.metered_network),
                getString(R.string.metered_network_confirm)
        ) {
            viewModel.onMeteredNetworkAllowed()
        }.show(requireFragmentManager(), "tag")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickDownload(mapListItem: OnlineMapListItem) = viewModel.initDownload(mapListItem)

    private fun onClickCancel(mapListItem: OnlineMapListItem) { viewModel.cancelDownload(mapListItem.status) }

    private fun onClickDelete(mapListItem: OnlineMapListItem) {
        WarningDialog(
                getString(R.string.delete_map),
                getString(R.string.confirm_delete_map, mapListItem.printName)
        ) {
            viewModel.deleteMap(mapListItem.id)
        }.show(requireFragmentManager(), null)
    }

    private fun browseMaps() = navigateTo(R.id.action_localMaps_to_browseMaps)

    private fun navigateToNext() {
        if (defaultSharedPrefs.get(sharedPrefsIsFirstRunKey, true))
            navigateTo(R.id.action_localMaps_to_map, createBundle(), R.id.localMapsFragment)
        else
            navigateBack()
    }

    private fun createBundle() = bundleOf(userIdKey to viewModel.userId)
}