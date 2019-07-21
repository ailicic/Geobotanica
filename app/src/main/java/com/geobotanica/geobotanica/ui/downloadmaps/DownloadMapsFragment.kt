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
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.databinding.FragmentDownloadMapsBinding
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.NOT_DOWNLOADED
import com.geobotanica.geobotanica.network.Geolocator
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.ui.dialog.WarningDialog
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.get
import com.geobotanica.geobotanica.util.getFromBundle
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_download_maps.*
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

// TODO: Show confirmation dialog for map deletion
// TODO: Improve animations?
// TODO: Reduce size of this class?
// TODO: Use bundle to pass downloaded maps ids

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class DownloadMapsFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<DownloadMapViewModel>
    private lateinit var viewModel: DownloadMapViewModel

    @Inject lateinit var networkValidator: NetworkValidator
    @Inject lateinit var storageHelper: StorageHelper
    @Inject lateinit var fileDownloader: FileDownloader
    @Inject lateinit var moshi: Moshi
    @Inject lateinit var geolocator: Geolocator
    @Inject lateinit var mapRepo: MapRepo

    private val mapListAdapter = MapListAdapter(::onClickFolder, ::onClickDownload, ::onClickCancel, ::onClickDelete)
    private val parentMapFolderIds = mutableListOf<Long>()

    // TODO: Put in viewModel
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
        viewModel.showFab.observe(this, Observer {
            Lg.d("showFab = $it")
        })
    }

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
        if (parentMapFolderIds.isNotEmpty()) {
            parentMapFolderIds.removeAt(parentMapFolderIds.size - 1)
            if (parentMapFolderIds.isEmpty())
//                viewModel.mapFolderId.value = null
                viewModel.browseMapFolder(null)
            else
                viewModel.browseMapFolder(parentMapFolderIds.last())
        } else if (isExitOnBackPressedOk())
            activity.finish()
    }

    private suspend fun isExitOnBackPressedOk(): Boolean {
        val isPermitted = defaultSharedPrefs.get(sharedPrefsExitOnBackInDownloadMaps, false)
        return if (! isPermitted) {
            suspendCoroutine { continuation ->
                WarningDialog(
                        R.string.exit_app    ,
                        R.string.exit_app_confirm,
                        sharedPrefsExitOnBackInDownloadMaps,
                        continuation
                ).show((activity as FragmentActivity).supportFragmentManager, "tag")
            }
        } else
            true
    }

    private fun bindClickListeners() {
//        browseMapsButton.setOnClickListener { browseMaps() }
//        showSuggestedMapsButton.setOnClickListener { showSuggestedMaps() }
//        getMapsButton.setOnClickListener { getSuggestedMaps() }
        fab.setOnClickListener { navigateToNext() }
    }


//    // TODO: Put in viewModel?
//    private fun getSuggestedMaps() = mainScope.launch {
//        if (networkValidator.isValid()) {
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
//        viewModel.mapFolderId.value = mapFolderItem.id
    }

    private fun onClickDownload(mapListItem: OnlineMapListItem) {
        Lg.d("onClickDownload(): ${mapListItem.filename}")
        mainScope.launch {
            if (networkValidator.isValid()) {
                fileDownloader.downloadMap(mapListItem)
                fab.isVisible = true
                // TODO: show spinner, then change icon after completed
            }
        }
//        if (mapListItem.isFolder) {
//            mutableListOf<OnlineMapListItem>().apply {
//                add(mapRepo.getChildFoldersOf(mapListItem.id). { it.to})
//            }
//            // TODO: Use live data map to supply child entries
////            parentMapFolders.add(mapListItem)
////            mapListAdapter.submitList(mapListItem.contents)
//        } else {
//            mainScope.launch {
//                if (networkValidator.isValid()) {
//                    fileDownloader.downloadMap(mapListItem)
//                    fab.isVisible = true
//                    // TODO: show spinner, then change icon after completed
//                }
//            }
//        }
    }


    private fun onClickCancel(mapListItem: OnlineMapListItem) {
        Lg.d("onClickCancel(): ${mapListItem.filename}")
        mainScope.launch(Dispatchers.IO) {
            fileDownloader.cancelDownload(downloadId = mapListItem.status)
            val onlineMap = mapRepo.get(mapListItem.id)
            onlineMap.status = NOT_DOWNLOADED
            mapRepo.update(onlineMap)
            Lg.i("Cancelled map download: ${onlineMap.filename}")
        }
    }

    private fun onClickDelete(mapListItem: OnlineMapListItem) {
        Lg.d("onClickDelete(): ${mapListItem.filename}")
        mainScope.launch(Dispatchers.IO) {
            val onlineMap = mapRepo.get(mapListItem.id)
            val mapFile = File(storageHelper.getMapsPath(), onlineMap.filename)
            val result = mapFile.delete()
            onlineMap.status = NOT_DOWNLOADED
            mapRepo.update(onlineMap)
            Lg.i("Deleted map: ${onlineMap.filename} (result=$result)")
        }
    }

//    @ExperimentalCoroutinesApi
//    private fun cancelDownload() {
//        Lg.i("DownloadTaxaFragment: Download cancelled.")
//        mainScope.cancel()
////        updateUi(CANCEL_DOWNLOAD)
//    }

    private fun navigateToNext() {
        val navController = activity.findNavController(R.id.fragment)
        navController.popBackStack()
        navController.navigate(R.id.mapFragment, createBundle())
    }

    private fun createBundle(): Bundle =
        bundleOf(userIdKey to viewModel.userId)
}


