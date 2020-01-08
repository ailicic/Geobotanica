package com.geobotanica.geobotanica.network.online_map

import com.geobotanica.geobotanica.android.file.StorageHelper
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.adapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


// JSoup selectors: https://jsoup.org/apidocs/org/jsoup/select/Selector.html
// Try/test JSoup: https://try.jsoup.org/

// TODO: Write tests for these classes. Stub out the JSON.

@Singleton
class OnlineMapScraper @Inject constructor (
        private val htmlParser: HtmlParser,
        private val moshi: Moshi,
        private val mapRepo: MapRepo,
        private val storageHelper: StorageHelper

) {
    private val mapsBaseUrl = "http://download.mapsforge.org/maps/v5/"

    init { scrape() }

    private fun scrape() = GlobalScope.launch(Dispatchers.IO) {
        fetchMaps(mapsBaseUrl)
        exportMapsToJsonFiles()
    }

    private suspend fun fetchMaps(baseUrl: String, parentFolderId: Long? = null) {
        withContext(Dispatchers.IO) {
            var fetchRetries = 5 // Fetch fails periodically on USA for some reason.
            try {
                val doc = htmlParser.parse(baseUrl)
                val rows = doc.select("tr")

                for (i in 3 until rows.size - 1) { // Skip first 3 rows and last row

                    val columns = rows[i].select("td")
                    val url = baseUrl + columns[1].select("a[href]").text()
                    val timestamp = columns[2].text()
                    val size = columns[3].text()
                            .replace("G", " GB")
                            .replace("M", " MB")

                    if (size == "-") {
                        val onlineMapFolder = OnlineMapFolder(url, timestamp, parentFolderId)
                        val folderId = mapRepo.insertFolder(onlineMapFolder)
                        Lg.d("Found folder: ${onlineMapFolder.printName}")
                        fetchMaps(url, folderId)
                    } else {
                        val onlineMap = OnlineMap(url, size, timestamp, parentFolderId)
                        mapRepo.insert(onlineMap)
                        Lg.d("Found map: ${onlineMap.printName}")
                    }
                }
            } catch (e: IOException) { // TODO: Try to prevent "java.io.IOException: Mark invalid"
                if (fetchRetries > 0) {
                    --fetchRetries
                    Lg.e("$baseUrl: fetchMaps() threw error: $e (retries = $fetchRetries)")
                    fetchMaps(baseUrl, parentFolderId)
                } else
                    throw e
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext") // Runs in coroutine on IO thread. No idea why it complains.
    private suspend fun exportMapsToJsonFiles() {
        val maps = mapRepo.getAll()
        val mapsAdapter = moshi.adapter<List<OnlineMap>>()
        val mapsJson = mapsAdapter.toJson(maps)
        Lg.d("mapsJson = $mapsJson")
//        val mapsSink = File(storageHelper.getDownloadPath(), onlineAssetList[MAP_LIST.ordinal].filename)
        val mapsSink = File(storageHelper.getDownloadPath(), "maps.json") // TODO: Remove hard-coded ref
                .sink().buffer()
        mapsSink.write(mapsJson.toByteArray())
        mapsSink.close()

        val mapFolders = mapRepo.getAllFolders()
        val folderAdapter = moshi.adapter<List<OnlineMapFolder>>()
        val foldersJson = folderAdapter.toJson(mapFolders)
        Lg.d("foldersJson = $foldersJson")
//        val foldersSink = File(storageHelper.getDownloadPath(), onlineAssetList[MAP_FOLDER_LIST.ordinal].filename)
        val foldersSink = File(storageHelper.getDownloadPath(), "map_folders.json") // TODO: Remove hard-coded ref
                .sink().buffer()
        foldersSink.write(foldersJson.toByteArray())
        foldersSink.close()
    }
}


@Singleton
class HtmlParser @Inject constructor() {
    fun parse(url: String): Document = Jsoup.connect(url).get()
}
