package com.rojas.syncpass.ui.event

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rojas.syncpass.domain.model.EventZone
import com.rojas.syncpass.domain.model.GeoPoint
import com.rojas.syncpass.domain.model.boundary
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Polygon
import org.maplibre.geojson.Point
import kotlin.math.log2

@Composable
fun EventZoneMap(point: GeoPoint?, radius: Int, onSelect: (GeoPoint) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val onPoint by rememberUpdatedState(onSelect)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var ready by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).also { it.onCreate(null) }
    }

    DisposableEffect(mapView, lifecycle) {
        var disposed = false
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { mapView.onStart() }
            override fun onResume(owner: LifecycleOwner) { mapView.onResume() }
            override fun onPause(owner: LifecycleOwner) { mapView.onPause() }
            override fun onStop(owner: LifecycleOwner) { mapView.onStop() }
        }
        lifecycle.addObserver(observer)
        mapView.addOnDidFailLoadingMapListener { _ ->
            if (!disposed) error = "El mapa no está disponible. Revisa la conexión o usa las coordenadas avanzadas"
        }
        mapView.getMapAsync { loadedMap ->
            if (!disposed) {
                map = loadedMap
                loadedMap.addOnMapClickListener { selected ->
                    if (!disposed) onPoint(GeoPoint(selected.latitude, selected.longitude))
                    true
                }
                loadedMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-33.45, -70.66), 11.0))
                loadedMap.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                    if (!disposed) {
                        style.addSource(GeoJsonSource("event-zone"))
                        style.addLayer(FillLayer("event-zone-fill", "event-zone").withProperties(
                            fillColor("#4f608e"), fillOpacity(0.3f), fillOutlineColor("#283d6b"),
                        ))
                        ready = true
                        error = null
                    }
                }
            }
        }
        onDispose {
            disposed = true
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(map, ready, point, radius) {
        point?.let {
            val zoom = (15.0 - log2(radius / 200.0)).coerceIn(2.0, 18.0)
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), zoom))
        }
    }
    LaunchedEffect(map, ready, point, radius) {
        if (ready && point != null) {
            val ring = EventZone(point, radius).boundary().map { Point.fromLngLat(it.longitude, it.latitude) }
            map?.style?.getSourceAs<GeoJsonSource>("event-zone")?.setGeoJson(Polygon.fromLngLats(listOf(ring)))
        }
    }
    Column {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxWidth().height(220.dp))
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
