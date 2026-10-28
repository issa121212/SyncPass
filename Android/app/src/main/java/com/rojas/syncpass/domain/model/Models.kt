package com.rojas.syncpass.domain.model

data class Proyecto(
    val id: Long,
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

data class Evento(
    val id: Long,
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

data class Perfil(
    val id: Int = 1,
    val usuarioId: Long,
    val nombre: String,
    val email: String,
    val roles: String,
    val qrContenido: String,
)

data class Recinto(
    val id: Long = 0,
    val nombre: String,
    val direccion: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val radioMetros: Int = 200,
) {
    fun validar() {
        require(nombre.isNotBlank() && nombre.trim().length <= 160) { "Nombre obligatorio (maximo 160 caracteres)" }
        require(direccion.isNotBlank() && direccion.trim().length <= 240) { "Direccion obligatoria (maximo 240 caracteres)" }
        require((latitud == null) == (longitud == null)) { "Ingresa ambas coordenadas o deja ambas vacias" }
        require(latitud == null || (latitud.isFinite() && latitud in -90.0..90.0)) { "Latitud invalida" }
        require(longitud == null || (longitud.isFinite() && longitud in -180.0..180.0)) { "Longitud invalida" }
        require(radioMetros > 0) { "El radio debe ser mayor que cero" }
    }
}


data class ProyectoInput(
    val nombre: String,
    val descripcion: String? = null,
    val areaTematica: String? = null,
    val unidadVecinal: String? = null,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val estado: String? = "PLANIFICADO",
)

data class EventoInput(
    val proyectoId: Long,
    val nombre: String,
    val descripcion: String? = null,
    val fechaHora: String,
    val cupos: Int = 0,
    val recinto: String? = null,
    val direccion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val radioMetros: Int = 200,
    val estado: String? = "PROGRAMADO",
)

data class Asistente(
    val usuarioId: Long = 0,
    val nombre: String = "",
    val email: String = "",
    val inscrito: Boolean = false,
    val presente: Boolean = false,
    val marcadoEn: String? = null,
    val encuesta: Encuesta? = null,
)

data class EncuestaEvento(
    val id: Long = 0,
    val eventoId: Long = 0,
    val estudiante: Persona? = null,
    val satisfaccion: Int = 0,
    val utilidad: Int = 0,
    val comentarios: String? = null,
    val enviadoEn: String? = null,
)

data class Persona(
    val id: Long = 0,
    val nombre: String = "",
    val email: String = "",
)

data class Encuesta(
    val id: Long = 0,
    val satisfaccion: Int = 0,
    val utilidad: Int = 0,
    val comentarios: String? = null,
    val enviadoEn: String? = null,
)
