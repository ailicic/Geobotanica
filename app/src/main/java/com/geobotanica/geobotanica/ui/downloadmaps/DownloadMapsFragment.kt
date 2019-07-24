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
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.databinding.FragmentDownloadMapsBinding
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.network.NetworkValidator.NetworkState.*
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.dialog.WarningDialog
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.get
import com.geobotanica.geobotanica.util.getFromBundle
import com.geobotanica.geobotanica.util.put
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_download_maps.*
import kotlinx.coroutines.*
import javax.inject.Inject

// TODO: Show confirmation dialog for map deletion
// TODO: Improve animations?

class DownloadMapsFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<DownloadMapViewModel>
    private lateinit var viewModel: DownloadMapViewModel

    @Inject lateinit var networkValidator: NetworkValidator

    private val mapListAdapter = MapListAdapter(::onClickFolder, ::onClickDownload, ::onClickCancel, ::onClickDelete)
    private val parentMapFolderIds = mutableListOf<Long>()

//    private lateinit var suggestedMapList: List<OnlineMapListItem>
//    private lateinit var geolocation: Geolocation

    private val mainScope = CoroutineScope(Dispatchers.Main) + Job()

    private val sharedPrefsExitOnBackInDownloadMaps = "exitOnBackPermitted"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            Lg.d("Fragment args: userId=$userId")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentDownloadMapsBinding>(
                layoutInflater, R.layout.fragment_download_maps, container, false).also {
            it.viewModel = viewModel
            it.lifecycleOwner = this
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addOnBackPressedCallback()
        bindClickListeners()
//        getSuggestedMaps()
        initRecyclerView()
    }

    @ExperimentalCoroutinesApi
    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }

    // TODO: Need to deregister after navigation?
    private fun addOnBackPressedCallback() {
        activity.toolbar.setNavigationOnClickListener { onClickBackButton() }
        requireActivity().onBackPressedDispatcher.addCallback(this) { onClickBackButton() }
    }

    private fun onClickBackButton() = mainScope.launch {
        if (parentMapFolderIds.isNotEmpty())
            browseParentFolder()
        else
            exitAppWithWarning()
    }

    private fun exitAppWithWarning() {
        val isPermitted = defaultSharedPrefs.get(sharedPrefsExitOnBackInDownloadMaps, false)
        if (isPermitted)
            activity.finish()
        else {
            WarningDialog(
                    R.string.exit_app,
                    R.string.exit_app_confirm,
                    {
                        defaultSharedPrefs.put(sharedPrefsExitOnBackInDownloadMaps to true)
                        activity.finish()
                    }
            ).show((activity as FragmentActivity).supportFragmentManager, "tag")
        }
    }

    private fun browseParentFolder() {
        parentMapFolderIds.removeAt(parentMapFolderIds.size - 1)
        if (parentMapFolderIds.isEmpty())
            viewModel.browseMapFolder(null)
        else
            viewModel.browseMapFolder(parentMapFolderIds.last())
    }

    private fun bindClickListeners() {
//        browseMapsButton.setOnClickListener { browseMaps() }
//        showSuggestedMapsButton.setOnClickListener { showSuggestedMaps() }
//        getMapsButton.setOnClickListener { getSuggestedMaps() }
        fab.setOnClickListener { navigateToNext() }
    }


//    private fun getSuggestedMaps() = mainScope.launch {
//        if (networkValidator.getStatus()) {
//            getMapsButton.isVisible = false
//            searchingOnlineMapsText.isVisible = true
//            progressBar.isVisible = true
//            try {
//                geolocation = geolocator.get()
////                suggestedMapList = onlineMapMatcher.findMapsFor(geolocation)
//            } catch (e: IOException) {
//                Lg.e("getSuggestedMaps(): Failed to geolocate.")
//            }
//            searchingOnlineMapsText.isVisible = false
//            progressBar.isVisible = false
//            initRecyclerView()
//        } else
//            getMapsButton.isVisible = true
//    }

    private fun initRecyclerView() {
        recyclerView.isVisible = true
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = mapListAdapter
        viewModel.mapListItems.observe(this, Observer {
            mapListAdapter.submitList(it)
        })
//        if (suggestedMapList.isNotEmpty())
//            showSuggestedMaps()
//        else
//            browseMaps()
    }

//    private fun showSuggestedMaps() {
//        browseMapsButton.isVisible = true
//        showSuggestedMapsButton.isVisible = false
//    }

//    private fun browseMaps() {
//        browseMapsButton.isVisible = false
//        showSuggestedMapsButton.isVisible = true
//        viewModel.mapListMode.value = BROWSING
//    }


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
                        R.string.metered_network,
                        R.string.metered_network_confirm,
                        { networkValidator.allowMeteredNetwork(); downloadMap(mapListItem) }
                ).show(requireFragmentManager(), "tag")
            }
        }
    }

    private fun downloadMap(mapListItem: OnlineMapListItem) {
        mainScope.launch {
            viewModel.downloadMap(mapListItem.id)
        }
    }

    private fun onClickCancel(mapListItem: OnlineMapListItem) {
        mainScope.launch {
            viewModel.cancelDownload(mapListItem.status)
        }
    }

    private fun onClickDelete(mapListItem: OnlineMapListItem) {
        mainScope.launch {
            viewModel.deleteMap(mapListItem.id)
        }
    }

    private fun navigateToNext() {
        val navController = activity.findNavController(R.id.fragment)
        navController.popBackStack()
        navController.navigate(R.id.mapFragment, createBundle())
    }

    private fun createBundle(): Bundle =
        bundleOf(userIdKey to viewModel.userId)
}


