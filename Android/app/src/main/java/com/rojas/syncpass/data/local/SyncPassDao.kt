package com.rojas.syncpass.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncPassDao {

    @Query("SELECT * FROM proyectos ORDER BY nombre")
    fun observarProyectos(): Flow<List<ProyectoEntity>>

    @Query("SELECT * FROM eventos ORDER BY fechaHora")
    fun observarEventos(): Flow<List<EventoEntity>>

    @Query("SELECT * FROM eventos WHERE id = :id")
    fun observarEvento(id: Long): Flow<EventoEntity?>

    @Query("SELECT * FROM perfil WHERE id = 1")
    fun observarPerfil(): Flow<PerfilEntity?>

    @Query("SELECT (SELECT COUNT(*) FROM pendientes WHERE sincronizado = 0 AND rechazado = 0) + (SELECT COUNT(*) FROM creaciones WHERE estado = 'PENDIENTE')")
    fun observarPendientes(): Flow<Int>

    @Query("SELECT * FROM pendientes WHERE sincronizado = 0 AND rechazado = 0 ORDER BY creadoEn")
    suspend fun pendientes(): List<PendienteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarProyectos(items: List<ProyectoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarEventos(items: List<EventoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarEvento(item: EventoEntity)

    @Query("DELETE FROM eventos WHERE id = :id")
    suspend fun eliminarEvento(id: Long)

    @Query("SELECT COUNT(*) FROM pendientes WHERE eventoId = :id AND sincronizado = 0 AND rechazado = 0")
    suspend fun pendientesDelEvento(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarPerfil(item: PerfilEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarPendiente(item: PendienteEntity)

    @Transaction
    suspend fun encolar(item: PendienteEntity) {
        guardarPendiente(item)
        reaplicarPendientes()
    }

    @Query("SELECT COUNT(*) FROM pendientes WHERE clientUuid = :uuid AND sincronizado = 0")
    suspend fun contarPendiente(uuid: String): Int

    @Query("SELECT * FROM pendientes WHERE clientUuid = :uuid")
    suspend fun operacion(uuid: String): PendienteEntity?

    @Query("DELETE FROM proyectos WHERE id = :id")
    suspend fun eliminarProyecto(id: Long)

    @Query("UPDATE eventos SET proyectoNombre = :nombre WHERE proyectoId = :id")
    suspend fun actualizarNombreProyecto(id: Long, nombre: String)

    @Transaction
    suspend fun actualizarProyecto(item: ProyectoEntity) {
        guardarProyectos(listOf(item))
        actualizarNombreProyecto(item.id, item.nombre)
    }

    @Query("DELETE FROM proyectos WHERE id > 0")
    suspend fun limpiarProyectos()

    @Query("DELETE FROM eventos WHERE id > 0")
    suspend fun limpiarEventos()

    @Query("UPDATE eventos SET inscrito = 1 WHERE id = :eventoId")
    suspend fun marcarInscrito(eventoId: Long)

    @Query("UPDATE eventos SET esPresente = :presente WHERE id = :eventoId")
    suspend fun actualizarPresencia(eventoId: Long, presente: Boolean)

    @Query("UPDATE eventos SET esPresente = 1 WHERE id = :eventoId")
    suspend fun marcarPresente(eventoId: Long)

    @Query("UPDATE eventos SET encuestaRespondida = 1 WHERE id = :eventoId")
    suspend fun marcarEncuesta(eventoId: Long)

    @Query("UPDATE pendientes SET sincronizado = 1, error = NULL WHERE clientUuid = :uuid")
    suspend fun marcarSincronizado(uuid: String)

    @Query("SELECT * FROM pendientes WHERE rechazado = 1 ORDER BY creadoEn")
    fun observarRechazadas(): Flow<List<PendienteEntity>>

    @Query("UPDATE pendientes SET rechazado = 1, error = :error WHERE clientUuid = :uuid")
    suspend fun marcarRechazado(uuid: String, error: String)

    @Query("UPDATE pendientes SET error = :error WHERE clientUuid = :uuid")
    suspend fun marcarError(uuid: String, error: String)

    @Query("DELETE FROM pendientes WHERE sincronizado = 1")
    suspend fun limpiarSincronizados()

    @Transaction
    suspend fun guardarEventoConPendientes(item: EventoEntity) {
        guardarEvento(item)
        reaplicarPendientes()
    }

    suspend fun reaplicarPendientes() {
        pendientes().forEach {
            when (it.tipo) {
                PendienteEntity.TIPO_INSCRIPCION -> marcarInscrito(it.eventoId)
                PendienteEntity.TIPO_ENCUESTA -> marcarEncuesta(it.eventoId)
            }
        }
    }

    @Transaction
    suspend fun reemplazarProyectos(items: List<ProyectoEntity>) {
        limpiarProyectos()
        guardarProyectos(items)
    }

    @Transaction
    suspend fun reemplazarEventos(items: List<EventoEntity>) {
        limpiarEventos()
        guardarEventos(items)
        reaplicarPendientes()
    }

    @Query("SELECT * FROM proyectos WHERE id = :id")
    suspend fun proyectoLocal(id: Long): ProyectoEntity?

    @Query("SELECT * FROM eventos WHERE id = :id")
    suspend fun eventoLocal(id: Long): EventoEntity?

    @Query("SELECT * FROM perfil WHERE id = 1")
    suspend fun perfilLocal(): PerfilEntity?

    @Insert
    suspend fun insertarCreacion(item: CreationEntity): Long

    @Query("SELECT * FROM creaciones WHERE estado = 'PENDIENTE' ORDER BY id")
    suspend fun creacionesPendientes(): List<CreationEntity>

    @Query("SELECT * FROM creaciones WHERE id = :id")
    suspend fun creacion(id: Long): CreationEntity?

    @Query("UPDATE creaciones SET remoteId = :remoteId, estado = :estado, error = :error WHERE id = :id")
    suspend fun estadoCreacion(id: Long, estado: String, remoteId: Long? = null, error: String? = null)

    @Query("UPDATE proyectos SET syncState = :estado, syncError = :error WHERE id = :id")
    suspend fun estadoProyecto(id: Long, estado: String, error: String?)

    @Query("UPDATE eventos SET syncState = :estado, syncError = :error WHERE id = :id")
    suspend fun estadoEvento(id: Long, estado: String, error: String?)

    @Query("UPDATE eventos SET proyectoId = :remoteId, proyectoNombre = :nombre WHERE proyectoId = :localId")
    suspend fun reasignarEventos(localId: Long, remoteId: Long, nombre: String)

    @Query("UPDATE creaciones SET proyectoId = :remoteId WHERE proyectoId = :localId")
    suspend fun reasignarCreaciones(localId: Long, remoteId: Long)

    @Transaction
    suspend fun encolarProyecto(creation: CreationEntity, item: ProyectoEntity): Long {
        val id = insertarCreacion(creation)
        guardarProyectos(listOf(item.copy(id = -id)))
        return id
    }

    @Transaction
    suspend fun encolarEvento(creation: CreationEntity, item: EventoEntity): Long {
        val id = insertarCreacion(creation)
        guardarEvento(item.copy(id = -id))
        return id
    }

    @Transaction
    suspend fun confirmarProyecto(id: Long, remote: ProyectoEntity) {
        guardarProyectos(listOf(remote))
        reasignarEventos(-id, remote.id, remote.nombre)
        reasignarCreaciones(-id, remote.id)
        eliminarProyecto(-id)
        estadoCreacion(id, "CONFIRMADO", remote.id)
    }

    @Transaction
    suspend fun confirmarEvento(id: Long, remote: EventoEntity) {
        guardarEvento(remote)
        eliminarEvento(-id)
        estadoCreacion(id, "CONFIRMADO", remote.id)
    }

    @Transaction
    suspend fun errorCreacion(item: CreationEntity, definitivo: Boolean, message: String) {
        val state = if (definitivo) "RECHAZADO" else "PENDIENTE"
        estadoCreacion(item.id, state, error = message)
        if (item.tipo == "PROYECTO") estadoProyecto(-item.id, state, message)
        else estadoEvento(-item.id, state, message)
    }
}
