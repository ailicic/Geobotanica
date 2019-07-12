package com.geobotanica.geobotanica.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.geobotanica.geobotanica.R
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.android.worker.DecompressionWorker
import com.geobotanica.geobotanica.android.worker.REMOTE_FILE_KEY
import com.geobotanica.geobotanica.data.entity.Location
import com.geobotanica.geobotanica.data_taxa.TaxaDatabaseValidator
import com.geobotanica.geobotanica.di.components.ApplicationComponent
import com.geobotanica.geobotanica.di.components.DaggerApplicationComponent
import com.geobotanica.geobotanica.di.modules.ApplicationModule
import com.geobotanica.geobotanica.di.modules.RepoModule
import com.geobotanica.geobotanica.network.FileDownloader
import com.geobotanica.geobotanica.network.onlineFileList
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.isEmulator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    lateinit var applicationComponent: ApplicationComponent
    @Inject lateinit var storageHelper: StorageHelper
    @Inject lateinit var fileDownloader: FileDownloader
    @Inject lateinit var taxaDatabaseValidator: TaxaDatabaseValidator

    val downloadComplete = MutableLiveData<String>()

    private val className = "MainActivity"

    private val downloadIdMap = mutableMapOf<Long, Int>() // <downloadId, remoteFileIndex>

    // TODO: Remove this after LocationService uses LiveData
    var currentLocation: Location? = null // Easy solution to hold location between fragments during new plant flow

    private var job = Job()
    private val mainScope = CoroutineScope(Dispatchers.Main) + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lg.v("$className: onCreate()")

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val navController = findNavController(R.id.fragment)
        setupActionBarWithNavController(this, navController)

        applicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(applicationContext, this))
                .repoModule(RepoModule())
                .build()

        applicationComponent.inject(this)

        AndroidGraphicFactory.createInstance(application) // Required by MapsForge
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        if (isEmulator())
            Lg.d("Running on emulator")
        else
            Lg.d("Running on device")
    }

    override fun onSupportNavigateUp() = findNavController(R.id.fragment).navigateUp()

    fun downloadAsset(remoteFileIndex: Int) {
        val downloadId = fileDownloader.download(onlineFileList[remoteFileIndex])
        downloadIdMap[downloadId] = remoteFileIndex
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadIdMap.containsKey(downloadId)) {
                val remoteFileIndex = downloadIdMap[downloadId]!!
                val remoteFile = onlineFileList[remoteFileIndex]
                if (storageHelper.isDownloaded(remoteFile)) {
                    Lg.i("Downloaded ${remoteFile.fileNameGzip}")
                    downloadIdMap.remove(downloadId)
                    val decompressionWorkerRequest = OneTimeWorkRequestBuilder<DecompressionWorker>()
                            .setInputData(workDataOf(REMOTE_FILE_KEY to remoteFileIndex))
                            .build()
                    val workManager = WorkManager.getInstance()
                    workManager.getWorkInfoByIdLiveData(decompressionWorkerRequest.id)
                            .observe(this@MainActivity, decompressionWorkerObserver)
                    workManager.enqueue(decompressionWorkerRequest)
                } else {
                    Lg.e("Error downloading ${remoteFile.description}")
                    Toast.makeText(applicationContext,
                            getString(R.string.error_downloading, remoteFile.description),
                            Toast.LENGTH_SHORT)
                            .show()
                }
            }
        }
    }

    private val decompressionWorkerObserver = Observer<WorkInfo> { info ->
        if (info != null && info.state.isFinished) {
            val remoteFileIndex = info.outputData.getInt(REMOTE_FILE_KEY, -1)
            val remoteFile = onlineFileList[remoteFileIndex]
            downloadComplete.value = remoteFile.fileName
            Lg.d("Decompressed: ${remoteFile.description}")

            if (remoteFile.fileName == "taxa.db") {
                mainScope.launch {
                    if (taxaDatabaseValidator.isPopulated())
                        Lg.d("isTaxaDbPopulated() = true")
                    else {
                        Lg.e("isTaxaDbPopulated() = false")
                        Toast.makeText(applicationContext,
                                getString(R.string.error_importing_plant_db),
                                Toast.LENGTH_SHORT)
                                .show()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Lg.v("$className: onStart()")
    }

    override fun onResume() {
        super.onResume()
        Lg.v("$className: onResume()")
    }

    override fun onPause() {
        super.onPause()
        Lg.v("$className: onPause()")
    }

    override fun onStop() {
        super.onStop()
        Lg.v("$className: onStop()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Lg.v("$className: onDestroy()")
        cancelDownloads()
        job.cancel()
        AndroidGraphicFactory.clearResourceMemoryCache()
        unregisterReceiver(onDownloadComplete)
    }

    private fun cancelDownloads() { // Note: DownloadManager deletes the file of a cancelled download
        if (downloadIdMap.isNotEmpty()) {
            val result = getSystemService<DownloadManager>()?.remove(*downloadIdMap.keys.toLongArray())
            Lg.d("MainActivity: Deleted $result downloads")
        }
    }
}