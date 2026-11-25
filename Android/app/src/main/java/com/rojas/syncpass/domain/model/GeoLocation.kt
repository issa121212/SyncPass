package com.rojas.syncpass.domain.model

data class GeoPoint(val latitude: Double, val longitude: Double) {
    fun validate() {
        require(latitude.isFinite() && latitude in -90.0..90.0) { "Latitud inválida" }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "Longitud inválida" }
    }
}

data class EventZone(val center: GeoPoint, val radiusMeters: Int) {
    fun validate() {
        center.validate()
        require(radiusMeters in 1..50_000) { "El radio debe estar entre 1 y 50000 metros" }
    }
}

data class DeviceLocation(
    val point: GeoPoint,
    val accuracyMeters: Double,
    val recordedAtMillis: Long,
) {
    fun validate(nowMillis: Long, maxAgeMillis: Long = 120_000) {
        point.validate()
        require(accuracyMeters.isFinite() && accuracyMeters > 0) { "Precisión de ubicación inválida" }
        require(recordedAtMillis > 0 && recordedAtMillis <= nowMillis && nowMillis - recordedAtMillis <= maxAgeMillis) {
            "La ubicación es antigua. Vuelve a intentarlo"
        }
    }
}
