package com.rojas.syncpass.domain.model

import kotlin.math.*

/** Contorno geográfico del radio, compartido por la vista previa del mapa. */
fun EventZone.boundary(): List<GeoPoint> {
    validate()
    val latitude = Math.toRadians(center.latitude)
    val longitude = Math.toRadians(center.longitude)
    val angularDistance = radiusMeters / 6_371_008.8
    val points = (0 until 64).map { index ->
        val bearing = index * 2.0 * PI / 64
        val targetLatitude = asin((sin(latitude) * cos(angularDistance) +
            cos(latitude) * sin(angularDistance) * cos(bearing)).coerceIn(-1.0, 1.0))
        val targetLongitude = longitude + atan2(
            sin(bearing) * sin(angularDistance) * cos(latitude),
            cos(angularDistance) - sin(latitude) * sin(targetLatitude),
        )
        GeoPoint(Math.toDegrees(targetLatitude), (Math.toDegrees(targetLongitude) + 540) % 360 - 180)
    }
    return points + points.first()
}
