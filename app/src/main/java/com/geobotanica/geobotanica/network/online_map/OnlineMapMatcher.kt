package com.geobotanica.geobotanica.network.online_map

import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import com.geobotanica.geobotanica.data.repo.MapRepo
import com.geobotanica.geobotanica.network.Geolocation
import com.geobotanica.geobotanica.ui.downloadmaps.OnlineMapListItem
import com.geobotanica.geobotanica.ui.downloadmaps.toListItem
import com.geobotanica.geobotanica.util.Lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


//@Singleton
//class OnlineMapMatcher @Inject constructor(
//        private val mapRepo: MapRepo
//) {
//
//    suspend fun findMapsFor(geolocation: Geolocation): List<OnlineMapListItem> = withContext(Dispatchers.IO) {
//        Lg.d("geolocation = $geolocation")
//        val maps = mapRepo.getAll()
//        val region = geolocation.region.toLowerCase().replace(' ', '-')
//        val regionResults = maps
//                .filter { it.filename.startsWith(region) }
//                .map { it.toListItem() }
//        Lg.d("OnlineMapMatcher: Matched ${regionResults.size} maps with $region")
//        return@withContext regionResults
//
//        // TODO: Match country if no regions. Show folders when appropriate

//        (onlineMapFolder, geolocation.region) // Province / state
////        val regionResults = findMapsFor(onlineMapFolder, geolocation.region) // Province / state
//
//        return if (regionResults.isNotEmpty())
//            regionResults
//        else
//            findMapsFor(onlineMapFolder, geolocation.countryName)
//    }

//    private fun findMapsFor(onlineMapFolder: OnlineMapListItem, _string: String): List<OnlineMapListItem> {
//        val string = _string.toLowerCase().replace(' ', '-')
//        val results = mutableListOf<OnlineMapListItem>()
//
//
//        onlineMapFolder.contents.forEach {
//            if (it.isFolder) {
//                if (it.filename.startsWith(string))
//                    results.addAll(it.contents.filter { !it.isFolder })
//                else
//                    results.addAll(findMapsFor(it, string))
//            }
//            else {
//                if (it.filename.startsWith(string))
//                    results.add(it)
//            }
//        }
//        return results
//    }
//}