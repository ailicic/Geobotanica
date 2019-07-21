package com.geobotanica.geobotanica.ui.downloadmaps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.Transformations.switchMap
import androidx.lifecycle.ViewModel
import com.geobotanica.geobotanica.data.entity.OnlineMap
import com.geobotanica.geobotanica.data.entity.OnlineMapFolder
import com.geobotanica.geobotanica.data.repo.MapRepo
import javax.inject.Inject
import javax.inject.Singleton

//const val VERNACULAR_COUNT = 25021 // TODO: Get from API
//const val TAXA_COUNT = 1103116 // TODO: Get from API
//const val VERNACULAR_TYPE_COUNT = 32201 // TODO: Get from API
//const val TAXA_TYPE_COUNT = 10340 // TODO: Get from API

@Singleton
class DownloadMapViewModel @Inject constructor(
        private val mapRepo: MapRepo
): ViewModel() {
    var userId = 0L

//    var mapListMode = MutableLiveData<MapListMode>().apply { value = SUGGESTED }

//    enum class MapListMode { SUGGESTED, BROWSING }

//    val mapListItems: LiveData<List<OnlineMapListItem>> = switchMap(mapListMode) { mode ->
//        when (mode) {
//            SUGGESTED ->{
//                map(mapRepo.search("British Columbia"
//
//                )) { mapList -> mapList.map { it.toListItem() } }
//            }
//            BROWSING -> switchMap(mapFolderId) { mapFolderId ->
//                childMapListItemsOf(mapFolderId)
//            }
//        }
//    }

//    mapRepo.search("British Columbia").observe(this, Observer {
//        mapListAdapter.submitList(it.map { onlineMap -> onlineMap.toListItem() })
//    })
    val showFab: LiveData<Boolean> = map(mapRepo.getInitiatedDownloads()) { mapList ->
        mapList.size > 0
    }

    private var mapFolderId = MutableLiveData<Long?>().apply { value = null }
//
    val mapListItems = switchMap(mapFolderId) { mapFolderId ->
        childMapListItemsOf(mapFolderId)
    }

    fun browseMapFolder(folderId: Long?) {
        mapFolderId.value = folderId
    }


    private fun childMapListItemsOf(mapFolderId: Long?): LiveData<List<OnlineMapListItem>> {

        val maps = mapRepo.getChildMapsOf(mapFolderId)
        val folders = mapRepo.getChildFoldersOf(mapFolderId)

        val result = MediatorLiveData<List<OnlineMapListItem>>()

        result.addSource(maps) {
//            Lg.d("OnlineMaps updated")
            result.value = combineMapListItems(folders.value, maps.value)
        }
        result.addSource(folders) {
//            Lg.d("OnlineMapFolders updated")
            result.value = combineMapListItems(folders.value, maps.value)
        }
        return result
    }

    private fun combineMapListItems(
            folders: List<OnlineMapFolder>?,
            maps: List<OnlineMap>?
    ): List<OnlineMapListItem> {

        val latestMaps = maps
        val latestFolders = folders

//        Lg.d("combineMapListItems(): latestMaps=$latestMaps; latestFolders=$latestFolders")
        if (latestMaps == null || latestFolders == null)
            return emptyList()

        return mutableListOf<OnlineMapListItem>().apply {
            addAll(latestFolders.map { it.toListItem() })
            addAll(latestMaps.map { it.toListItem() })
        }
    }
}