package com.rojas.syncpass.domain

import com.rojas.syncpass.domain.model.*
import kotlinx.coroutines.flow.Flow

data class OperacionRechazada(val tipo: String, val eventoId: Long, val motivo: String)
interface ProyectosRepository {
    val proyectos: Flow<List<Proyecto>>
    suspend fun consultarProyecto(id: Long): Result<Proyecto>
    suspend fun crearProyecto(request: ProyectoInput): Result<Unit>
    suspend fun editarProyecto(id: Long, request: ProyectoInput): Result<Unit>
    suspend fun eliminarProyecto(id: Long): Result<Unit>
}
interface RecintosRepository {
    val recintos: Flow<List<Recinto>>
    suspend fun guardarRecinto(item: Recinto): Result<Unit>
    suspend fun eliminarRecinto(id: Long): Result<Unit>
}
interface SyncPassOperations : ProyectosRepository, RecintosRepository {
    val eventos: Flow<List<Evento>>
    val perfil: Flow<Perfil?>
    val pendientes: Flow<Int>
    val rechazadas: Flow<List<OperacionRechazada>>
    fun observarEvento(id: Long): Flow<Evento?>
    suspend fun refrescar(): Result<Unit>
    suspend fun refrescarEvento(id: Long): Result<Evento>
    suspend fun inscribirse(evento: Evento): Result<Unit>
    suspend fun registrarAsistenciaEscaneo(eventoId: Long, codigoQr: String, latitud: Double?, longitud: Double?, precisionMetros: Double?): Result<Boolean>
    suspend fun guardarEncuesta(eventoId: Long, satisfaccion: Int, utilidad: Int, comentarios: String?): Result<Unit>
    suspend fun crearEvento(request: EventoInput): Result<Evento>
    suspend fun asistentes(eventoId: Long): Result<List<Asistente>>
    suspend fun encuestas(eventoId: Long): Result<List<EncuestaEvento>>
    suspend fun sincronizar(): Result<Int>
    fun cerrarSesionLocal()
}
