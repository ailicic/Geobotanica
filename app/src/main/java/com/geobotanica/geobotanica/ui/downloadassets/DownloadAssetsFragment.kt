package com.geobotanica.geobotanica.ui.downloadassets

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.entity.OnlineAsset
import com.geobotanica.geobotanica.data.entity.OnlineAssetId
import com.geobotanica.geobotanica.data.repo.AssetRepo
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DECOMPRESSING
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.DOWNLOADED
import com.geobotanica.geobotanica.network.FileDownloader.DownloadStatus.NOT_DOWNLOADED
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.network.online_map.OnlineMapScraper
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import kotlinx.android.synthetic.main.fragment_download_assets.*
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

// TODO: Move stuff to VM. Clean up.

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class DownloadAssetsFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<DownloadAssetsViewModel>
    private lateinit var viewModel: DownloadAssetsViewModel

    @Inject lateinit var storageHelper: StorageHelper
    @Inject lateinit var networkValidator: NetworkValidator
    @Inject lateinit var fileDownloader: FileDownloader
    @Inject lateinit var assetRepo: AssetRepo
    @Inject lateinit var mapRepo: MapRepo
    @Inject lateinit var mapScraper: OnlineMapScraper

    private var job = Job()
    private val mainScope = CoroutineScope(Dispatchers.Main) + job

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            Lg.d("Fragment args: userId=$userId")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_download_assets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainScope.launch {
//            mapScraper.scrape(); return@launch
            bindClickListeners()
            importOnlineAssetInfo()
            monitorAssetDownloads()
            initUi()
        }
    }

    private suspend fun importOnlineAssetInfo() = withContext(Dispatchers.IO) {
        if (assetRepo.isEmpty())
            assetRepo.insert(onlineAssetList)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    @SuppressLint("UsableSpace")
    private suspend fun initUi() {
        worldMapText.text = withContext(Dispatchers.IO) {
            assetRepo.get(OnlineAssetId.WORLD_MAP.id).printName
        }
        plantNameDbText.text = withContext(Dispatchers.IO) {
            assetRepo.get(OnlineAssetId.PLANT_NAMES.id).printName
        }
        internalStorageText.text = getString(R.string.internal_storage,
                File(context?.filesDir?.absolutePath).usableSpace / 1024 / 1024)
    }

    private fun bindClickListeners() {
        downloadButton.setOnClickListener(::onClickDownload)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickDownload(view: View?) {
        mainScope.launch {
            if (networkValidator.isValid()) {
                downloadButton.isVisible = false
                progressBar.isVisible = true
                downloadAssets()
            }
        }
    }

    private suspend fun downloadAssets() = withContext(Dispatchers.IO) {
        assetRepo.getAll().forEach { asset ->
            if (asset.isDownloading) {
                Lg.d("Asset already downloading: ${asset.filenameGzip}")
                return@forEach
            } else if (asset.status == DECOMPRESSING) {
                Lg.d("Asset already decompressing: ${asset.filenameGzip}")
                return@forEach
            } else if (asset.status == DOWNLOADED) {
                Lg.d("Asset already available: ${asset.filename}")
                return@forEach
            } else if (!storageHelper.isStorageAvailable(asset)) {
                showStorageErrorSnackbar(asset)
                return@forEach
            } else
                fileDownloader.downloadAsset(asset)
        }
    }

    private suspend fun monitorAssetDownloads() {
        val onlineAssets = withContext(Dispatchers.IO) { assetRepo.getAllLiveData() }
        onlineAssets.observe(this@DownloadAssetsFragment, Observer { assets ->
            mainScope.launch() {
                val mapFoldersAsset = assets.find { it.id == OnlineAssetId.MAP_FOLDER_LIST.id }!!
                val mapListAsset = assets.find { it.id == OnlineAssetId.MAP_LIST.id }!!
                val worldMapAsset = assets.find { it.id == OnlineAssetId.WORLD_MAP.id }!!
                val plantNamesAsset = assets.find { it.id == OnlineAssetId.PLANT_NAMES.id }!!
                if (mapFoldersAsset.status == DOWNLOADED && mapListAsset.status == DOWNLOADED &&
                        worldMapAsset.status != NOT_DOWNLOADED && plantNamesAsset.status != NOT_DOWNLOADED)
                {
                    Lg.d("DownloadAssetsFragment: Map data imported and asset downloads initialized -> navigateToNext()")
                    navigateToNext()
                }
            }
        })
    }

    private fun showStorageErrorSnackbar(asset: OnlineAsset) {
        Lg.i("Error: Insufficient storage for ${asset.description}")
        if (asset.isInternalStorage) {
            showSnackbar(R.string.not_enough_internal_storage, R.string.Inspect) {
                startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            }
        } else {
            showSnackbar(R.string.not_enough_external_storage, R.string.Inspect) {
                startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            }
        }
    }


    private suspend fun navigateToNext() = withContext(Dispatchers.Main) {
        val navController = activity.findNavController(R.id.fragment)
        navController.popBackStack()
        navController.navigate(R.id.downloadMapsFragment, createBundle())
    }

    private fun createBundle(): Bundle =
        bundleOf(userIdKey to viewModel.userId)


    // TODO: Get from API
    private val onlineAssetList = listOf(
        OnlineAsset(
            "Map metadata",
            "http://people.okanagan.bc.ca/ailicic/Maps/map_folders.json.gz",
            "",
            false,
            353,
            1_407
        ),
        OnlineAsset(
            "Map list",
            "http://people.okanagan.bc.ca/ailicic/Maps/maps.json.gz",
            "",
            false,
            5_792,
            42_619
        ),
        OnlineAsset(
            "World map",
            "http://people.okanagan.bc.ca/ailicic/Maps/world.map.gz",
            "maps",
            false,
            2_715_512,
            3_276_950
        ),
        OnlineAsset(
            "Plant name database",
            "http://people.okanagan.bc.ca/ailicic/Markers/taxa.db.gz",
            "databases",
            true,
            29_038_255,
            129_412_096
        )
    )
}