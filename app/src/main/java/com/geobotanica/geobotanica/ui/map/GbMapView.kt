package com.geobotanica.geobotanica.ui.map

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import com.geobotanica.geobotanica.android.location.Location
import com.geobotanica.geobotanica.ui.map.marker.PlantMarker
import com.geobotanica.geobotanica.util.Lg
import com.geobotanica.geobotanica.util.SingleLiveEvent
import kotlinx.android.synthetic.main.fragment_map.view.*
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.datastore.MultiMapDataStore
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.labels.LabelLayer
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.InternalRenderTheme
import org.mapsforge.map.scalebar.MapScaleBar
import org.mapsforge.map.view.InputListener
import java.io.File


class GbMapView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : MapView(context, attrs) {

    val reloadMarkers = SingleLiveEvent<Unit>()

    private lateinit var tileRendererLayer: TileRendererLayer
    private lateinit var tileCache: TileCache
    private val loadedMaps = mutableListOf<String>()

    fun init(mapFiles: List<File>, latitude: Double, longitude: Double, zoomLevel: Int) {
        isClickable = true
        mapScaleBar.isVisible = true
        mapScaleBar.scaleBarPosition = MapScaleBar.ScaleBarPosition.TOP_LEFT
        setBuiltInZoomControls(true)
        setCenter(LatLong(latitude, longitude))
        setZoomLevel(zoomLevel.toByte())
        mapZoomControls.zoomControlsGravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL


        tileCache = AndroidUtil.createTileCache(context, "mapcache",
                model.displayModel.tileSize, 1f, model.frameBufferModel.overdrawFactor, true)
        Lg.v("tileCache: capacity = ${tileCache.capacity}, capacityFirstLevel = ${tileCache.capacityFirstLevel}")

        tileRendererLayer = createTileRenderLayer(mapFiles)
        layerManager.layers.add(tileRendererLayer)

        val labelLayer = LabelLayer(AndroidGraphicFactory.INSTANCE, tileRendererLayer.labelStore)
        layerManager.layers.add(labelLayer)

        registerZoomListener()
    }

    private fun createTileRenderLayer(mapFiles: List<File>): TileRendererLayer {
        return object : TileRendererLayer(
                tileCache, createMultiMapDataStore(mapFiles),
                model.mapViewPosition,
                false, false, true,
                AndroidGraphicFactory.INSTANCE)
        {
            override fun onTap(tapLatLong: LatLong?, layerXY: Point?, tapXY: Point?): Boolean {
                Lg.d("onTap")
                return if (hideAnyMarkerInfoBubble())
                    true
                else
                    super.onTap(tapLatLong, layerXY, tapXY)
            }
        }.apply { setXmlRenderTheme(InternalRenderTheme.DEFAULT) }
    }

    private fun createMultiMapDataStore(mapFiles: List<File>): MultiMapDataStore {
        val multiMapDataStore = MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL)

        loadedMaps.clear()
        mapFiles.forEach { mapFile ->
            Lg.d("Loading downloaded map: ${mapFile.name}")
            loadedMaps.add(mapFile.name)
            multiMapDataStore.addMapDataStore(MapFile(mapFile), false, false)
        }
        return multiMapDataStore
    }

    private fun registerZoomListener() {
        addInputListener(object : InputListener {
            override fun onZoomEvent() {
                Lg.d("New zoom level = ${mapView.model.mapViewPosition.zoomLevel}")
            }

            override fun onMoveEvent() { }
        })
    }

    fun isMapLoaded(filename: String) = loadedMaps.contains(filename)

    fun reloadMaps(mapFiles: List<File>) {
        Lg.d("MapFragment: reloadMaps()")
        tileRendererLayer.onDestroy()
        tileCache.purge() // Delete all cache files. If omitted, existing cache will override new maps.
        tileRendererLayer = createTileRenderLayer(mapFiles)
        layerManager.layers.clear()
        layerManager.layers.add(tileRendererLayer)
        reloadMarkers.call()
    }

    fun hideAnyMarkerInfoBubble(): Boolean {
        layerManager.layers.forEach { layer ->
            if (layer is PlantMarker && layer.isShowingMarkerBubble) {
                layer.hideInfoBubble()
                return true
            }
        }
        return false
    }

    fun centerMap(location: Location, animate: Boolean = true) {
        if (isLocationOffScreen(location)) {
            if (animate)
                model.mapViewPosition.animateTo(location.toLatLong())
            else
                setCenter(location.toLatLong())
        }
    }

    private fun isLocationOffScreen(location: Location): Boolean = ! boundingBox.contains(location.toLatLong())
}