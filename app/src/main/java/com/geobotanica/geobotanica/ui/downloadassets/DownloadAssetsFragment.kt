package com.geobotanica.geobotanica.ui.downloadassets

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.DownloadManager.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data_taxa.TaxaDatabaseValidator
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.network.OnlineAsset
import com.geobotanica.geobotanica.network.OnlineAssetIndex.*
import com.geobotanica.geobotanica.network.onlineAssetList
import com.geobotanica.geobotanica.ui.BaseFragment
import com.geobotanica.geobotanica.ui.BaseFragmentExt.getViewModel
import com.geobotanica.geobotanica.ui.ViewModelFactory
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.getFromBundle
import kotlinx.android.synthetic.main.fragment_download_assets.*
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject


@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class DownloadAssetsFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory<DownloadAssetsViewModel>
    private lateinit var viewModel: DownloadAssetsViewModel

    @Inject lateinit var storageHelper: StorageHelper
    @Inject lateinit var networkValidator: NetworkValidator
    @Inject lateinit var fileDownloader: FileDownloader
    @Inject lateinit var taxaDatabaseValidator: TaxaDatabaseValidator

    private var job = Job()
    private val mainScope = CoroutineScope(Dispatchers.Main) + job

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity.applicationComponent.inject(this)

        viewModel = getViewModel(viewModelFactory) {
            userId = getFromBundle(userIdKey)
            Lg.d("Fragment args: userId=$userId")
        }

        // TODO: Remove this after login screen is implemented (conditional navigation should start there)
        mainScope.launch {
            if (areAssetsDownloaded()) {
                Lg.d("DownloadAssetsFragment: Assets already downloaded. Navigating to next...")
                navigateToNext()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_download_assets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
        bindClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    @SuppressLint("UsableSpace")
    private fun initUi() {
        worldMapText.text = onlineAssetList[WORLD_MAP.ordinal].descriptionWithSize
        plantNameDbText.text = onlineAssetList[PLANT_NAMES.ordinal].descriptionWithSize
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

    private fun downloadAssets() {
        registerMapsListDownloadedObserver()
        onlineAssetList.forEachIndexed { onlineFileIndex: Int, it: OnlineAsset ->
            if (isDownloadActive(it)) {
                Lg.d("Download already active: ${it.description}")
                return@forEachIndexed
            }
            if (storageHelper.isAssetDecompressed(it)) { // True if already downloaded and decompressed
                Lg.d("Asset already available: ${it.description}")
                if (it.fileName == onlineAssetList[MAPS_LIST.ordinal].fileName)
                    navigateToNext()
                return@forEachIndexed
            }
            if (!storageHelper.isStorageAvailable(it)) {
                showStorageErrorSnackbar(it)
                return@forEachIndexed
            }
            activity.downloadAsset(onlineFileIndex)
        }
    }

    private fun isDownloadActive(onlineFile: OnlineAsset): Boolean {
        val query = Query().setFilterByStatus(
                STATUS_PENDING or STATUS_RUNNING or STATUS_PAUSED)
        val cursor = appContext.getSystemService<DownloadManager>()!!.query(query)
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)) == onlineFile.descriptionWithSize)
                return true
        }
        return false
    }

    private fun registerMapsListDownloadedObserver() {
        activity.assetDownloadComplete.observe(this, Observer { onlineAssetIndex ->
            if (onlineAssetIndex == MAPS_LIST)
                navigateToNext()
        })
    }

    private fun showStorageErrorSnackbar(onlineFile: OnlineAsset) {
        Lg.i("Error: Insufficient storage for ${onlineFile.description}")
        if (onlineFile.isInternalStorage) {
            showSnackbar(R.string.not_enough_internal_storage, R.string.Inspect) {
                startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            }
        } else {
            showSnackbar(R.string.not_enough_external_storage, R.string.Inspect) {
                startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            }
        }
    }

    private suspend fun areAssetsDownloaded(): Boolean {
        var areDownloaded = true
        onlineAssetList.forEach {
            if (!storageHelper.isAssetDecompressed(it))
                areDownloaded = false
        }
        if (areDownloaded)
            areDownloaded = taxaDatabaseValidator.isPopulated()
        return areDownloaded
    }

    private fun navigateToNext() {
        val navController = activity.findNavController(R.id.fragment)
        navController.popBackStack()
        navController.navigate(R.id.downloadMapsFragment, createBundle())
    }

    private fun createBundle(): Bundle =
        bundleOf(userIdKey to viewModel.userId)
}