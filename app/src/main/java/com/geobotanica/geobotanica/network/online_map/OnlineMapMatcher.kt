package com.geobotanica.geobotanica.network.online_map

import com.geobotanica.geobotanica.network.Geolocation
import com.geobotanica.geobotanica.util.Lg
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class OnlineMapMatcher @Inject constructor() {

    fun search(onlineMapFolder: OnlineMapFolder, geolocation: Geolocation): List<OnlineMapFile> {
        Lg.d("geolocation = $geolocation")
        val regionResults = search(onlineMapFolder, geolocation.region) // Province / state

        return if (regionResults.isNotEmpty())
            regionResults
        else
            search(onlineMapFolder, geolocation.countryName)
    }

    private fun search(onlineMapFolder: OnlineMapFolder, _string: String): List<OnlineMapFile> {
        val string = _string.toLowerCase().replace(' ', '-')
        val results = mutableListOf<OnlineMapFile>()


        onlineMapFolder.contents.forEach {
            if (it is OnlineMapFolder) {
                if (it.name.startsWith(string))
                    results.addAll(it.contents.filterIsInstance<OnlineMapFile>())
                else
                    results.addAll(search(it, string))
            }
            else if (it is OnlineMapFile) {
                if (it.name.startsWith(string))
                    results.add(it)
            }
        }
        return results
    }
}