package com.rojas.syncpass

import com.rojas.syncpass.domain.model.*
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class GeoLocationTest {
    @Test fun contornoCerradoMantieneElRadioEnMetrosAlrededorDelRecinto() {
        val center = GeoPoint(-33.45, -70.66)
        val ring = EventZone(center, 250).boundary()
        assertEquals(ring.first(), ring.last())
        ring.forEach { point ->
            point.validate()
            val lat1 = Math.toRadians(center.latitude)
            val lat2 = Math.toRadians(point.latitude)
            val a = sin((lat2 - lat1) / 2).pow(2) + cos(lat1) * cos(lat2) *
                sin(Math.toRadians(point.longitude - center.longitude) / 2).pow(2)
            val distance = 6_371_008.8 * 2 * atan2(sqrt(a), sqrt(1 - a))
            assertEquals(250.0, distance, 0.01)
        }
    }

    @Test fun precisionInvalidaYFechasFueraDeRangoSeRechazan() {
        val base = DeviceLocation(GeoPoint(0.0, 0.0), 8.0, 1_000_000)
        base.validate(1_000_000)
        for (invalid in listOf(
            base.copy(point = GeoPoint(Double.NaN, 0.0)),
            base.copy(point = GeoPoint(0.0, 181.0)),
            base.copy(accuracyMeters = Double.POSITIVE_INFINITY),
            base.copy(accuracyMeters = 0.0),
            base.copy(recordedAtMillis = 0),
            base.copy(recordedAtMillis = 1_000_001),
            base.copy(recordedAtMillis = 879_999),
        )) assertTrue(runCatching { invalid.validate(1_000_000) }.isFailure)
    }
}
