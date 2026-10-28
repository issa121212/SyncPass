package com.rojas.syncpass.data.remote

data class UsuarioResponse(
    val id: Long = 0,
    val cognitoSub: String = "",
    val email: String = "",
    val nombre: String = "",
    val roles: List<String> = emptyList(),
)

data class ResponsableResponse(
    val id: Long = 0,
    val nombre: String = "",
    val email: String = "",
)

data class ProyectoResponse(
    val id: Long = 0,
    val nombre: String = "",
    val descripcion: String? = null,
    val areaTematica: String? = null,
    val unidadVecinal: String? = null,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val estado: String = "PLANIFICADO",
    val responsable: ResponsableResponse? = null,
    val creadoEn: String? = null,
    val actualizadoEn: String? = null,
)

data class EventoResponse(
    val id: Long = 0,
    val proyectoId: Long = 0,
    val proyectoNombre: String = "",
    val nombre: String = "",
    val descripcion: String? = null,
    val fechaHora: String = "",
    val cupos: Int = 0,
    val inscritos: Int = 0,
    val presentes: Int = 0,
    val recinto: String? = null,
    val direccion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val radioMetros: Int = 200,
    val qrContenido: String? = null,
    val estado: String = "PROGRAMADO",
    val creadoEn: String? = null,
    val actualizadoEn: String? = null,
    val inscrito: Boolean = false,
    val esPresente: Boolean = false,
    val encuestaRespondida: Boolean = false,
)

data class InscripcionResponse(
    val id: Long = 0,
    val eventoId: Long = 0,
    val usuario: ResponsableResponse? = null,
    val estado: String = "",
    val creadoEn: String? = null,
)

data class EncuestaResponse(
    val id: Long = 0,
    val satisfaccion: Int = 0,
    val utilidad: Int = 0,
    val comentarios: String? = null,
    val enviadoEn: String? = null,
)

data class EncuestaEventoResponse(
    val id: Long = 0,
    val eventoId: Long = 0,
    val estudiante: ResponsableResponse? = null,
    val satisfaccion: Int = 0,
    val utilidad: Int = 0,
    val comentarios: String? = null,
    val enviadoEn: String? = null,
)

data class AsistenciaResponse(
    val id: Long = 0,
    val eventoId: Long = 0,
    val estudiante: ResponsableResponse? = null,
    val esPresente: Boolean = false,
    val marcadoEn: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val precisionMetros: Double? = null,
    val origen: String = "",
    val encuesta: EncuestaResponse? = null,
)

data class AsistenteResponse(
    val usuarioId: Long = 0,
    val nombre: String = "",
    val email: String = "",
    val inscrito: Boolean = false,
    val presente: Boolean = false,
    val marcadoEn: String? = null,
    val encuesta: EncuestaResponse? = null,
)

data class QrResponse(val contenido: String = "")

data class SyncResult(
    val procesados: Int = 0,
    val errores: Int = 0,
    val mensajes: List<String> = emptyList(),
)

data class InscribirseRequest(val codigoQr: String)

data class AsistenciaRequest(
    val codigoQr: String? = null,
    val esPresente: Boolean? = true,
    val marcadoEn: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val precisionMetros: Double? = null,
    val clientUuid: String? = null,
)

data class EncuestaRequest(
    val satisfaccion: Int,
    val utilidad: Int,
    val comentarios: String? = null,
    val enviadoEn: String? = null,
)

data class ProyectoRequest(
    val nombre: String,
    val descripcion: String? = null,
    val areaTematica: String? = null,
    val unidadVecinal: String? = null,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val estado: String? = "PLANIFICADO",
)

data class EventoRequest(
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

data class AsistenciaSyncItem(
    val clientUuid: String,
    val eventoId: Long,
    val codigoQr: String,
    val marcadoEn: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val precisionMetros: Double? = null,
)

data class EncuestaSyncItem(
    val eventoId: Long,
    val satisfaccion: Int,
    val utilidad: Int,
    val comentarios: String? = null,
    val enviadoEn: String? = null,
)
