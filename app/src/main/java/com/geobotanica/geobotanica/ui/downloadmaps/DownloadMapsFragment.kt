package com.geobotanica.geobotanica.ui.downloadmaps

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.network.Geolocation
import com.geobotanica.geobotanica.network.Geolocator
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.network.OnlineFileIndex.MAPS_LIST
import com.geobotanica.geobotanica.network.onlineFileList
import com.geobotanica.geobotanica.network.online_map.OnlineMapEntry
import com.geobotanica.geobotanica.network.online_map.OnlineMapMatcher
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.adapter
import com.geobotanica.geobotanica.util.getFromBundle
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_download_maps.*
import kotlinx.coroutines.*
import okio.buffer
import okio.source
import java.io.File
import java.io.IOException
import javax.inject.Inject

// TODO: Improve animations?
// TODO: Reduce size of this class?
// TODO: Use viewModel to pass downloaded maps

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class DownloadMapsFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<DownloadMapViewModel>
    private lateinit var viewModel: DownloadMapViewModel

    @Inject lateinit var networkValidator: NetworkValidator
    @Inject lateinit var storageHelper: StorageHelper
    @Inject lateinit var moshi: Moshi
    @Inject lateinit var geolocator: Geolocator
    @Inject lateinit var onlineMapMatcher: OnlineMapMatcher

    private lateinit var onlineMapList: OnlineMapEntry
    private lateinit var suggestedMapList: List<OnlineMapEntry>
    private lateinit var geolocation: Geolocation

    private val mapListAdapter = MapListAdapter(::onClickMapEntry)
    private val parentMapFolders = mutableListOf<OnlineMapEntry>()

    private val mainScope = CoroutineScope(Dispatchers.Main) + Job()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            Lg.d("Fragment args: userId=$userId")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_download_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addOnBackPressedCallback()
        bindClickListeners()
        deserializeMapsList()
        getSuggestedMaps()
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

    private fun onClickBackButton() {
        if (parentMapFolders.size > 1) {
            parentMapFolders.removeAt(parentMapFolders.size - 1)
            mapListAdapter.submitList(parentMapFolders.last().contents)
        } else {
            // TODO: Show dialog confirming user wants to exit
            activity.onBackPressed()
        }
    }

    private fun bindClickListeners() {
        browseMapsButton.setOnClickListener { browseMaps() }
        showSuggestedMapsButton.setOnClickListener { showSuggestedMaps() }
        getMapsButton.setOnClickListener { getSuggestedMaps() }
        fab.setOnClickListener { navigateToNext() }
    }

    private fun deserializeMapsList() {
        val mapsListOnlineFile = onlineFileList[MAPS_LIST.ordinal]
        val mapsListFile = File(storageHelper.getLocalPath(mapsListOnlineFile), mapsListOnlineFile.fileName)
        if (mapsListFile.exists() && mapsListFile.length() == mapsListOnlineFile.decompressedSize) {
            try {
                val source = mapsListFile.source().buffer()
                val mapsListJson = source.readUtf8()
                source.close()
                val adapter = moshi.adapter<OnlineMapEntry>()
                onlineMapList = adapter.fromJson(mapsListJson) as OnlineMapEntry
            } catch (e: IOException){
                Lg.e("deserializeMapsList(): $e")
                mapsListFile.delete()
                handleUnexpectedMapsListEror()
            }
        } else {
            Lg.e("deserializeMapsList(): Maps file absent or size error.")
            mapsListFile.delete()
            handleUnexpectedMapsListEror()
        }
    }

    private fun handleUnexpectedMapsListEror() {
        showToast("Error getting maps")
        val navController = activity.findNavController(R.id.fragment)
        navController.popBackStack()
        navController.navigate(R.id.downloadAssetsFragment, createBundle())
    }

    private fun getSuggestedMaps() = mainScope.launch {
        if (networkValidator.isValid()) {
            getMapsButton.isVisible = false
            searchingOnlineMapsText.isVisible = true
            progressBar.isVisible = true
            try {
                geolocation = geolocator.get()
            } catch (e: IOException) {
                Lg.e("getSuggestedMaps(): Failed to geolocate.")
                getMapsButton.isVisible = true
                searchingOnlineMapsText.isVisible = false
                progressBar.isVisible = false
            }
            suggestedMapList = onlineMapMatcher.search(onlineMapList, geolocation)
            searchingOnlineMapsText.isVisible = false
            progressBar.isVisible = false
            initRecyclerView()
        } else
            getMapsButton.isVisible = true
    }

    private fun initRecyclerView() {
        recyclerView.isVisible = true
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = mapListAdapter
        if (suggestedMapList.isNotEmpty())
            showSuggestedMaps()
        else
            browseMaps()
    }

    private fun showSuggestedMaps() {
        mapListAdapter.submitList(suggestedMapList)
        browseMapsButton.isVisible = true
        showSuggestedMapsButton.isVisible = false
    }

    private fun browseMaps() {
        browseMapsButton.isVisible = false
        showSuggestedMapsButton.isVisible = true
        parentMapFolders.clear()
        parentMapFolders.add(onlineMapList)
        mapListAdapter.submitList(onlineMapList.contents)
    }

    private fun onClickMapEntry(onlineMapEntry: OnlineMapEntry) {
        if (onlineMapEntry.isFolder) {
            parentMapFolders.add(onlineMapEntry)
            mapListAdapter.submitList(onlineMapEntry.contents)
        } else {
            fab.isVisible = true
            // TODO: Download map, show spinner, then change icon after completed
        }
    }

    @ExperimentalCoroutinesApi
    private fun cancelDownload() {
        Lg.i("DownloadTaxaFragment: Download cancelled.")
        mainScope.cancel()
//        updateUi(CANCEL_DOWNLOAD)
    }

    private fun navigateToNext() {
        val navController = activity.findNavController(R.id.fragment)
        navController.popBackStack()
        navController.navigate(R.id.mapFragment, createBundle())
    }

    private fun createBundle(): Bundle =
        bundleOf(userIdKey to viewModel.userId)

// TODO: Run this on server periodically to update maps.json.gz asset
//
//    private fun scrapeMaps() {
//        mainScope.launch {
//            lateinit var onlineMaps: OnlineMapEntry
//            val time = measureTimeMillis {
//                onlineMaps = onlineMapScraper.scrape() // TODO: Handle IOException. Show toast.
//            }
//            Lg.d("onlineMapScraper took $time ms")
//
//            searchingOnlineMapsText.isVisible = false
//            progressBar.isVisible = false
//            showToast("Got maps")
//
//            saveMapsList(onlineMaps)
//        }
//    }
//
//
//    private fun saveMapsList(onlineMaps: OnlineMapEntry) {
//        val adapter = moshi.adapter<OnlineMapEntry>()
//        val mapsJson = adapter.toJson(onlineMaps)
//        Lg.d("mapsJson = $mapsJson")
//
//        val sink = File(storageHelper.getDownloadPath(),"maps.json").sink().buffer()
//        sink.write(mapsJson.toByteArray())
//        sink.close()
//    }
}


