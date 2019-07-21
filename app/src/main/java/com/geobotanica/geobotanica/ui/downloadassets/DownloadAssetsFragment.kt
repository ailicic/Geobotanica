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
//    @Inject lateinit var mapScraper: OnlineMapScraper

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
        downloadButton.setOnClickListener(::onClickDownload)
        bindViewModel()
        initUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun bindViewModel() {
        with(viewModel) {

            navigateToNext.observe(this@DownloadAssetsFragment, Observer {
                if (it) {
                    Lg.d("DownloadAssetsFragment: Map data imported and asset downloads initialized -> navigateToNext()")
                    navigateToNext()
                }
            })

            showStorageSnackbar.observe(this@DownloadAssetsFragment, Observer { asset ->
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
            })
        }
    }

    @SuppressLint("UsableSpace")
    private fun initUi() = mainScope.launch {
        worldMapText.text = withContext(Dispatchers.IO) {
            assetRepo.get(OnlineAssetId.WORLD_MAP.id).printName
        }
        plantNameDbText.text = withContext(Dispatchers.IO) {
            assetRepo.get(OnlineAssetId.PLANT_NAMES.id).printName
        }
        internalStorageText.text = getString(R.string.internal_storage,
                File(context?.filesDir?.absolutePath).usableSpace / 1024 / 1024)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickDownload(view: View?) {
        mainScope.launch {
            if (networkValidator.isValid()) {
                downloadButton.isVisible = false
                progressBar.isVisible = true
                viewModel.downloadAssets()
            }
        }
    }

    private fun navigateToNext() {
        val navController = activity.findNavController(R.id.fragment)
        navController.popBackStack()
        navController.navigate(R.id.downloadMapsFragment, createBundle())
    }

    private fun createBundle(): Bundle =
        bundleOf(userIdKey to viewModel.userId)
}