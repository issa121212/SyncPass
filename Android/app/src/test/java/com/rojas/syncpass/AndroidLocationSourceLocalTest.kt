package com.rojas.syncpass

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.rojas.syncpass.data.location.AndroidLocationSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AndroidLocationSourceLocalTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val manager get() = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val clock = Clock.fixed(Instant.ofEpochMilli(1_000_000),ZoneOffset.UTC)
    private fun point(provider: String,time: Long,accuracy: Float) = Location(provider).apply {
        latitude = -33.45; longitude = -70.66; this.time = time; this.accuracy = accuracy
    }

    @Test fun cacheInvalidaDeGpsNoDescartaUbicacionValidaDeRed() = runBlocking {
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER,true)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER,true)
        shadowOf(manager).setLastKnownLocation(LocationManager.GPS_PROVIDER,point("gps",clock.millis()+1,3f))
        shadowOf(manager).setLastKnownLocation(LocationManager.NETWORK_PROVIDER,point("network",clock.millis()-1000,40f))
        val location = AndroidLocationSource(context,clock).currentLocation().getOrThrow()
        assertEquals(40.0,location.accuracyMeters,0.0)
        assertEquals(clock.millis()-1000,location.recordedAtMillis)
    }

    @Test fun permisoAproximadoUsaRedYNoSeleccionaGpsAunqueEsteEnCache() = runBlocking {
        shadowOf(context).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER,true)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER,true)
        shadowOf(manager).setLastKnownLocation(LocationManager.GPS_PROVIDER,point("gps",clock.millis(),3f))
        shadowOf(manager).setLastKnownLocation(LocationManager.NETWORK_PROVIDER,point("network",clock.millis()-1000,60f))
        assertEquals(60.0,AndroidLocationSource(context,clock).currentLocation().getOrThrow().accuracyMeters,0.0)
    }

    @Test fun permisosDenegadosYUbicacionDesactivadaDanMensajesRecuperables() = runBlocking {
        shadowOf(context).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)
        val source = AndroidLocationSource(context,clock)
        assertTrue(source.currentLocation().exceptionOrNull()?.message.orEmpty().contains("Permite la ubicación"))
        shadowOf(context).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER,false)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER,false)
        assertTrue(source.currentLocation().exceptionOrNull()?.message.orEmpty().contains("Activa la ubicación"))
    }
}
