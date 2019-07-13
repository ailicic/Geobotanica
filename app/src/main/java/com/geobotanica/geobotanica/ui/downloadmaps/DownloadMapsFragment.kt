package com.geobotanica.geobotanica.ui.downloadmaps

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.network.Geolocation
import com.geobotanica.geobotanica.network.Geolocator
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.network.OnlineFileIndex.MAPS_LIST
import com.geobotanica.geobotanica.network.onlineFileList
import com.geobotanica.geobotanica.network.online_map.OnlineMapEntry
import com.geobotanica.geobotanica.network.online_map.OnlineMapMatcher
import com.geobotanica.geobotanica.network.online_map.OnlineMapScraper
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.adapter
import com.geobotanica.geobotanica.util.getFromBundle
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.fragment_download_maps.*
import kotlinx.coroutines.*
import okio.buffer
import okio.source
import java.io.File
import java.io.IOException
import javax.inject.Inject


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
    private lateinit var geolocation: Geolocation

    private var mainScope = CoroutineScope(Dispatchers.Main) + Job() // Need var to re-instantiate after cancellation

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
        bindClickListeners()
        deserializeMapsList()
        getSuggestedMaps()
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
            showSuggestedMaps()
        } else
            getMapsButton.isVisible = true
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }

    @SuppressLint("UsableSpace")
    private fun showSuggestedMaps() {
        val results = onlineMapMatcher.search(onlineMapList, geolocation)
        results.forEach {
            Lg.d("Match = $it")
        }
        searchingOnlineMapsText.isVisible = false
        progressBar.isVisible = false
        // TODO: Populate recyclerview
    }

    private fun bindClickListeners() {
        getMapsButton.setOnClickListener { getSuggestedMaps() }
//        fab.setOnClickListener(::onClickFab)
    }

    @ExperimentalCoroutinesApi
    private fun cancelDownload() {
        Lg.i("DownloadTaxaFragment: Download cancelled.")
        mainScope.cancel()
        mainScope += Job() // TODO: Required to launch another coroutine in this scope. Better approach?
//        updateUi(CANCEL_DOWNLOAD)
    }

//    @Suppress("UNUSED_PARAMETER")
//    private fun onClickTrash(view: View?) {
//        val file = File(viewModel.databasesPath, "gb.db")
//        Lg.d("Deleting gb.db (Result = ${file.delete()}")
////        updateUi(DELETE_DOWNLOAD)
//    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickFab(view: View?) {
        navigateToNext()
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


