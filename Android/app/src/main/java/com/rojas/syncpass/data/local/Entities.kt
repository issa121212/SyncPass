package com.rojas.syncpass.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proyectos")
data class ProyectoEntity(
    @PrimaryKey val id: Long,
    val nombre: String,
    val descripcion: String?,
    val areaTematica: String?,
    val unidadVecinal: String?,
    val fechaInicio: String?,
    val fechaFin: String?,
    val estado: String,
    val responsableNombre: String?,
    val responsableEmail: String?,
    val responsableId: Long? = null,
)

@Entity(tableName = "eventos")
data class EventoEntity(
    @PrimaryKey val id: Long,
    val proyectoId: Long,
    val proyectoNombre: String,
    val nombre: String,
    val descripcion: String?,
    val fechaHora: String,
    val cupos: Int,
    val inscritos: Int,
    val presentes: Int,
    val recinto: String?,
    val direccion: String?,
    val latitud: Double?,
    val longitud: Double?,
    val radioMetros: Int,
    val qrContenido: String?,
    val estado: String,
    val inscrito: Boolean,
    val esPresente: Boolean,
    val encuestaRespondida: Boolean,
    val actualizadoEn: String?,
)

@Entity(tableName = "perfil")
data class PerfilEntity(
    @PrimaryKey val id: Int = 1,
    val usuarioId: Long,
    val nombre: String,
    val email: String,
    val roles: String,
    val qrContenido: String,
)

/**
 * Cola de operaciones capturadas en terreno. Se guarda primero en Room y luego
 * se envía al backend; si falla, queda pendiente para el próximo sync.
 */
@Entity(tableName = "pendientes")
data class PendienteEntity(
    @PrimaryKey val clientUuid: String,
    val tipo: String,
    val eventoId: Long,
    val codigoQr: String?,
    val marcadoEn: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val precisionMetros: Double? = null,
    val satisfaccion: Int? = null,
    val utilidad: Int? = null,
    val comentarios: String? = null,
    val creadoEn: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false,
    val error: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val rechazado: Boolean = false,
) {
    companion object {
        const val TIPO_ASISTENCIA_ESCANEO = "ASISTENCIA_ESCANEO"
        const val TIPO_ASISTENCIA_PROPIA = "ASISTENCIA_PROPIA"
        const val TIPO_AUSENCIA_PROPIA = "AUSENCIA_PROPIA"
        const val TIPO_ENCUESTA = "ENCUESTA"
        const val TIPO_INSCRIPCION = "INSCRIPCION"
    }
}

/** Locally maintained business catalog. These records have no API endpoint. */
@Entity(tableName = "recintos_locales")
data class RecintoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val direccion: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val radioMetros: Int = 200,
)
