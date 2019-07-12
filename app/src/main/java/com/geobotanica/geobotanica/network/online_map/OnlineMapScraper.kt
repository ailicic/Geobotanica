package com.geobotanica.geobotanica.network.online_map

import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


// JSoup selectors: https://jsoup.org/apidocs/org/jsoup/select/Selector.html
// Try/test JSoup: https://try.jsoup.org/

// TODO: Write tests for these classes. Stub out the JSON.

@Singleton
class OnlineMapScraper @Inject constructor (private val htmlParser: HtmlParser) {
    private val mapsBaseUrl = "http://download.mapsforge.org/maps/v5"

    suspend fun scrape(): OnlineMapFolder {
        return OnlineMapFolder(mapsBaseUrl).apply {
            contents.addAll(fetchEntries(mapsBaseUrl))
        }
    }

    private suspend fun fetchEntries(url: String): List<OnlineMapEntry> {
        var fetchRetries = 5 // Fetch fails periodically on USA for some reason.
        try {
            val doc = withContext(Dispatchers.IO) { htmlParser.parse(url) }
            val rows = doc.select("tr")
            val entries = mutableListOf<OnlineMapEntry>()

            for (i in 3 until rows.size - 1) { // Skip first 3 rows and last row

                val columns = rows[i].select("td")
                val name = columns[1].select("a[href]").text().removeSuffix("/")
                val size = columns[3].text()
                        .replace("G", " GB")
                        .replace("M", " MB")

                if (size == "-") {
                    val onlineMapFolder = OnlineMapFolder("$url/$name")
//                    Lg.d("Found folder: $onlineMapFolder")
                    onlineMapFolder.contents.addAll(fetchEntries(onlineMapFolder.url))
                    entries.add(onlineMapFolder)
                } else {
//                    val onlineMapFile = OnlineMapFile("$url/$name", size)
//                    Lg.d("Found file: $onlineMapFile")
                    entries.add(OnlineMapFile("$url/$name", size))
                }
            }

            return entries
        } catch (e: IOException) { // TODO: Try to prevent "java.io.IOException: Mark invalid"
            if (fetchRetries > 0) {
                --fetchRetries
                Lg.e("$url: fetchEntries() threw error: $e (retries = $fetchRetries)")
                return fetchEntries(url)
            } else
                throw e
        }
    }
}


@Singleton
class HtmlParser @Inject constructor() {
    fun parse(url: String) = Jsoup.connect(url).get()
}