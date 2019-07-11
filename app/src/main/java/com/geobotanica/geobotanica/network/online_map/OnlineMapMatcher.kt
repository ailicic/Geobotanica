package com.geobotanica.geobotanica.network.online_map

import com.geobotanica.geobotanica.network.Geolocator
import com.geobotanica.geobotanica.util.Lg
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class OnlineMapMatcher @Inject constructor(
        val geolocator: Geolocator
) {

    suspend fun search(remoteMapFolder: OnlineMapFolder): List<OnlineMapFile> {
        val geolocation = geolocator.get()
        Lg.d("geolocation = $geolocation")
        val regionResults = search(remoteMapFolder, geolocation.region) // Province / state

        return if (regionResults.isNotEmpty())
            regionResults
        else
            search(remoteMapFolder, geolocation.countryName)
    }

    private fun search(remoteMapFolder: OnlineMapFolder, _string: String): List<OnlineMapFile> {
        val string = _string.toLowerCase().replace(' ', '-')
        val results = mutableListOf<OnlineMapFile>()


        remoteMapFolder.contents.forEach {
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