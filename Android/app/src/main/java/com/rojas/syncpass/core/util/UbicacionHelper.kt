package com.rojas.syncpass.core.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

data class Ubicacion(
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Double?,
)

class UbicacionHelper(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun ultimaUbicacion(): Ubicacion? {
        val permisoFino = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val permisoGrueso = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!permisoFino && !permisoGrueso) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val candidatos = listOfNotNull(
            runCatching { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull(),
            runCatching { manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull(),
            runCatching { manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) }.getOrNull(),
        )
        val mejor: Location = candidatos.maxByOrNull { it.time } ?: return null
        return Ubicacion(
            latitud = mejor.latitude,
            longitud = mejor.longitude,
            precisionMetros = if (mejor.hasAccuracy()) mejor.accuracy.toDouble() else null,
        )
    }
}
