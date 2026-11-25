package com.rojas.syncpass.data.repository

import com.rojas.syncpass.data.local.PendienteEntity
import com.rojas.syncpass.data.local.SyncPassDao
import com.rojas.syncpass.data.remote.resultado
import com.rojas.syncpass.domain.model.AttendanceCapture
import com.rojas.syncpass.domain.model.AttendanceOutcome
import java.time.Instant
import java.util.UUID

/** Persiste la captura completa antes de intentar enviarla; no obtiene otra posición al reintentar. */
class RoomAsistenciasRepository(
    private val dao: SyncPassDao,
    private val synchronize: suspend () -> Result<Int>,
) {
    suspend fun capture(eventId: Long, qr: String, capture: AttendanceCapture): Result<AttendanceOutcome> = resultado {
        val uuid = UUID.randomUUID().toString()
        dao.encolar(PendienteEntity(
            clientUuid = uuid, tipo = PendienteEntity.TIPO_ASISTENCIA_ESCANEO,
            eventoId = eventId, codigoQr = qr, marcadoEn = capture.capturedAt.toString(),
            latitud = capture.location?.point?.latitude, longitud = capture.location?.point?.longitude,
            precisionMetros = capture.location?.accuracyMeters,
            ubicacionEn = capture.location?.let { Instant.ofEpochMilli(it.recordedAtMillis).toString() },
        ))
        synchronize()
        val operation = dao.operacion(uuid)
        when {
            operation == null || operation.sincronizado -> AttendanceOutcome.Confirmed
            operation.rechazado -> AttendanceOutcome.Rejected(operation.error ?: "Captura rechazada")
            else -> AttendanceOutcome.Pending
        }
    }
}
