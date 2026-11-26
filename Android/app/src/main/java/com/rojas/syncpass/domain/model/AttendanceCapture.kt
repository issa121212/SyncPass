package com.rojas.syncpass.domain.model

import java.time.Instant
import kotlin.math.*

/** La posición pertenece al dispositivo receptor, no al participante del QR. */
data class AttendanceCapture(val capturedAt: Instant, val location: DeviceLocation?)

sealed interface AttendanceOutcome {
    data object Confirmed : AttendanceOutcome
    data object Pending : AttendanceOutcome
    data class Rejected(val reason: String) : AttendanceOutcome
}

object AttendanceLocationPolicy {
    fun validate(zone: EventZone, capture: AttendanceCapture) {
        zone.validate()
        val location = requireNotNull(capture.location) { "Se necesita la ubicación del dispositivo receptor" }
        location.validate(capture.capturedAt.toEpochMilli())
        require(location.accuracyMeters <= minOf(100.0, zone.radiusMeters / 2.0)) {
            "La ubicación es imprecisa. Activa la ubicación precisa y vuelve a intentarlo"
        }
        require(distance(zone.center, location.point) + location.accuracyMeters <= zone.radiusMeters) {
            "Acércate al centro de la zona y vuelve a intentar el escaneo"
        }
    }

    fun distance(first: GeoPoint, second: GeoPoint): Double {
        first.validate()
        second.validate()
        val lat1 = Math.toRadians(first.latitude)
        val lat2 = Math.toRadians(second.latitude)
        val a = sin((lat2 - lat1) / 2).pow(2) + cos(lat1) * cos(lat2) *
            sin(Math.toRadians(second.longitude - first.longitude) / 2).pow(2)
        return 6_371_008.8 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
}
