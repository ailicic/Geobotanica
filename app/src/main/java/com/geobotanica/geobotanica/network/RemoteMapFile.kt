package com.geobotanica.geobotanica.network

import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException


open class RemoteMapEntry(val name: String)

//@JsonClass(generateAdapter = true)
class RemoteMapFile(name: String) : RemoteMapEntry(name)

//@JsonClass(generateAdapter = true)
class RemoteMapFolder(name: String, val parent: RemoteMapFolder? = null) : RemoteMapEntry(name) {
    private val url: String
    private val contents = mutableListOf<RemoteMapEntry>()

    init {
        val parentUrl = parent?.let {it.url + "/" } ?: ""
        url = parentUrl + name
    }

    var fetchRetries = 3

    suspend fun fetchEntries() {
        try {
            val doc = withContext(Dispatchers.IO) { Jsoup.connect(url).get() }
            val links = doc.select("a[href]")
            val entries = mutableListOf<RemoteMapEntry>()

            Lg.d("fetchEntries($url)")
            links.forEach { link ->
                val href = link.text()
                if (href.endsWith(".map")) {
                    Lg.d("Found file: $href")
                    entries.add(RemoteMapFile(href))
                } else if (href.endsWith('/')) {
                    val remoteFolder = RemoteMapFolder(href.removeSuffix("/"), this)
                    Lg.d("Found folder: ${remoteFolder.name}")
                    remoteFolder.fetchEntries()
                    entries.add(remoteFolder)
                }
            }
            contents.addAll(entries)
            fetchRetries = 3
        } catch (e: IOException) {
            if (fetchRetries > 0) {
                --fetchRetries
                Lg.e("$name: fetchEntries() threw error: $e (retries = $fetchRetries)")
                fetchEntries()
            } else
                throw e

        }
    }

    fun search(geolocation: Geolocation): List<RemoteMapFile> {
        val regionResults = search(geolocation.region)
        return if (regionResults.isNotEmpty())
            regionResults // Province / state
        else
            search(geolocation.countryName)
    }

    fun search(_string: String): List<RemoteMapFile> {
        val string = _string.toLowerCase().replace(' ', '-')
        val results = mutableListOf<RemoteMapFile>()

        contents.forEach {
            if (it is RemoteMapFolder)
                results.addAll(it.search(string))
            else if (it is RemoteMapFile && it.name.contains(string))
                results.add(it)
        }
        return results
    }
}

