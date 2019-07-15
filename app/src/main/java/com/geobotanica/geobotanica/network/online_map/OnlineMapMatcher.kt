package com.geobotanica.geobotanica.network.online_map

import com.geobotanica.geobotanica.network.Geolocation
import com.geobotanica.geobotanica.util.Lg
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class OnlineMapMatcher @Inject constructor() {

    fun search(onlineMapFolder: OnlineMapEntry, geolocation: Geolocation): List<OnlineMapEntry> {
        Lg.d("geolocation = $geolocation")
        val regionResults = search(onlineMapFolder, geolocation.region) // Province / state

        return if (regionResults.isNotEmpty())
            regionResults
        else
            search(onlineMapFolder, geolocation.countryName)
    }

    private fun search(onlineMapFolder: OnlineMapEntry, _string: String): List<OnlineMapEntry> {
        val string = _string.toLowerCase().replace(' ', '-')
        val results = mutableListOf<OnlineMapEntry>()


        onlineMapFolder.contents.forEach {
            if (it.isFolder) {
                if (it.filename.startsWith(string))
                    results.addAll(it.contents.filter { !it.isFolder })
                else
                    results.addAll(search(it, string))
            }
            else {
                if (it.filename.startsWith(string))
                    results.add(it)
            }
        }
        return results
    }
}