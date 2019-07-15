package com.geobotanica.geobotanica.network

import android.app.DownloadManager
import android.net.Uri
import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.network.online_map.OnlineMapEntry
import com.geobotanica.geobotanica.util.Lg
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class FileDownloader @Inject constructor (
        private val storageHelper: StorageHelper,
        private val networkValidator: NetworkValidator,
        private val downloadManager: DownloadManager
) {
    fun downloadAsset(onlineFile: OnlineAsset): Long {
        val file = File(storageHelper.getDownloadPath(), onlineFile.fileNameGzip)

        val request = DownloadManager.Request(Uri.parse(onlineFile.url))
                .setTitle(onlineFile.descriptionWithSize)
                .setDescription("Downloading")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setVisibleInDownloadsUi(false)

                // TODO: Add these options to a preferences page
                .setAllowedOverMetered(networkValidator.isNetworkMetered()) // Warning dialog handles metered network permission.
                .setAllowedOverRoaming(false) // True by default.

        Lg.i("Started asset download: ${onlineFile.description}")
        return downloadManager.enqueue(request)
    }

    fun downloadMap(onlineMapEntry: OnlineMapEntry): Long {
        val file = File(storageHelper.getMapsPath(), onlineMapEntry.filename)

        val request = DownloadManager.Request(Uri.parse(onlineMapEntry.url))
                .setTitle(onlineMapEntry.printName)
                .setDescription("Downloading")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setVisibleInDownloadsUi(false)

                // TODO: Add these options to a preferences page
                .setAllowedOverMetered(networkValidator.isNetworkMetered()) // Warning dialog handles metered network permission.
                .setAllowedOverRoaming(false) // True by default.

        Lg.i("Started map download: ${onlineMapEntry.printName}")
        return downloadManager.enqueue(request)
    }
}