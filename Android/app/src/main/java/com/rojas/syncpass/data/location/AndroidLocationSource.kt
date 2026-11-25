package com.rojas.syncpass.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.rojas.syncpass.data.remote.resultado
import com.rojas.syncpass.domain.LocationSource
import com.rojas.syncpass.domain.model.DeviceLocation
import com.rojas.syncpass.domain.model.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import javax.inject.Inject
import kotlin.coroutines.resume

class AndroidLocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : LocationSource {
    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): Result<DeviceLocation> = resultado {
        val finePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarsePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        require(finePermission || coarsePermission) { "Permite la ubicación o selecciona el lugar en el mapa" }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { it != LocationManager.GPS_PROVIDER || finePermission }
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        require(providers.isNotEmpty()) { "Activa la ubicación del dispositivo" }
        val cached = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.toDomain()
            ?.takeIf { runCatching { it.validate(clock.millis()) }.isSuccess }
        val location = cached ?: withTimeoutOrNull(12_000) {
            suspendCancellableCoroutine { continuation ->
                val cancellation = CancellationSignal()
                continuation.invokeOnCancellation { cancellation.cancel() }
                try {
                    LocationManagerCompat.getCurrentLocation(
                        manager, providers.first(), cancellation, ContextCompat.getMainExecutor(context),
                    ) { result -> if (continuation.isActive) continuation.resume(result?.toDomain()) }
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
        requireNotNull(location) { "No se pudo obtener ubicación. Reintenta o selecciona el punto en el mapa" }
            .also { it.validate(clock.millis()) }
    }

    private fun Location.toDomain(): DeviceLocation = DeviceLocation(
        point = GeoPoint(latitude, longitude),
        accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else Double.NaN,
        recordedAtMillis = time,
    )
}
