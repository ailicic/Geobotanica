package com.geobotanica.geobotanica.network

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.ui.MainActivity
import com.geobotanica.geobotanica.util.Lg
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FileDownloader @Inject constructor (
        activity: MainActivity,
        private val storageHelper: StorageHelper,
        private val networkValidator: NetworkValidator
) {
    private var downloadManager: DownloadManager =
            activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun download(remoteFile: OnlineFile): Long {
        val file = File(storageHelper.getDownloadPath(), remoteFile.fileNameGzip)

        val request = DownloadManager.Request(Uri.parse(remoteFile.url))
                .setTitle(remoteFile.descriptionWithSize)
                .setDescription("Downloading")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setVisibleInDownloadsUi(false)

                // TODO: Add these options to a preferences page
                .setAllowedOverMetered(networkValidator.isNetworkMetered()) // Warning dialog handles metered network permission.
                .setAllowedOverRoaming(false) // True by default.

        Lg.i("Started download: ${remoteFile.description}")
        return downloadManager.enqueue(request)
    }
}