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
import androidx.navigation.findNavController
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data_taxa.TaxaDatabaseValidator
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.NetworkValidator
import com.geobotanica.geobotanica.network.OnlineFile
import com.geobotanica.geobotanica.network.onlineFileList
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
        // TODO: Dynamically add text views for list of files to download
        worldMapText.text = onlineFileList[0].descriptionWithSize
        plantNameDbText.text = onlineFileList[1].descriptionWithSize
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
                onlineFileList.forEachIndexed { onlineFileIndex: Int, it: OnlineFile ->
                    if (storageHelper.isDecompressed(it)) { // True if already downloaded and decompressed
                        Lg.d("Skipping download: ${it.description}")
                        return@forEachIndexed
                    }
                    if (!storageHelper.isStorageAvailable(it)) {
                        showStorageErrorSnackbar(it)
                        return@forEachIndexed
                    }
                    activity.downloadAsset(onlineFileIndex)
                }
                navigateToNext()
            }
        }
    }

    private fun showStorageErrorSnackbar(onlineFile: OnlineFile) {
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
        onlineFileList.forEach {
            if (!storageHelper.isDecompressed(it))
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