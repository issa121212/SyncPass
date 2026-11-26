package com.rojas.syncpass

import com.rojas.syncpass.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class AttendanceLocationPolicyTest {
    private val now = Instant.parse("2026-11-25T12:00:00Z")
    private val zone = EventZone(GeoPoint(-33.45, -70.66), 200)
    private val location = DeviceLocation(zone.center, 10.0, now.toEpochMilli())

    @Test fun permiteUnaUbicacionRecienteConMargenCompletoDentroDelRadio() {
        AttendanceLocationPolicy.validate(zone, AttendanceCapture(now, location))
        val nearLimit = zone.copy(radiusMeters = 180).boundary().first()
        AttendanceLocationPolicy.validate(zone, AttendanceCapture(now, location.copy(point = nearLimit, accuracyMeters = 19.0)))
        assertTrue(runCatching {
            AttendanceLocationPolicy.validate(zone, AttendanceCapture(now, location.copy(point = nearLimit, accuracyMeters = 21.0)))
        }.isFailure)
    }

    @Test fun noAmpliaLaZonaConLaPrecisionNiAceptaPosicionesFueraDelRecinto() {
        val outside = zone.copy(radiusMeters = 210).boundary().first()
        assertTrue(runCatching { AttendanceLocationPolicy.validate(zone, AttendanceCapture(now, location.copy(point = outside))) }.isFailure)
        assertTrue(runCatching { AttendanceLocationPolicy.validate(zone, AttendanceCapture(now, location.copy(accuracyMeters = 101.0))) }.isFailure)
        assertTrue(runCatching { AttendanceLocationPolicy.validate(zone.copy(radiusMeters = 20), AttendanceCapture(now, location.copy(accuracyMeters = 11.0))) }.isFailure)
    }

    @Test fun ubicacionAusenteOVencidaNoPermiteCapturarPeroElLimiteDeDosMinutosSi() {
        assertTrue(runCatching { AttendanceLocationPolicy.validate(zone, AttendanceCapture(now, null)) }.isFailure)
        AttendanceLocationPolicy.validate(zone, AttendanceCapture(now, location.copy(recordedAtMillis = now.toEpochMilli() - 120000)))
        assertTrue(runCatching { AttendanceLocationPolicy.validate(zone, AttendanceCapture(now, location.copy(recordedAtMillis = now.toEpochMilli() - 120001))) }.isFailure)
    }

    @Test fun calculoDeDistanciaSoportaAntimeridianoYPuntosOpuestos() {
        assertEquals(0.0, AttendanceLocationPolicy.distance(zone.center, zone.center), 0.001)
        assertEquals(22239.0, AttendanceLocationPolicy.distance(GeoPoint(0.0, 179.9), GeoPoint(0.0, -179.9)), 1.0)
        assertEquals(20015114.4, AttendanceLocationPolicy.distance(GeoPoint(0.0, 0.0), GeoPoint(0.0, 180.0)), 1.0)
    }
}
