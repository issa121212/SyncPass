package com.rojas.syncpass.data.local

/** Frontera local sustituible por memoria en pruebas unitarias. Room aplica las transacciones. */
interface CreationStore {
    suspend fun profile(): PerfilEntity?
    suspend fun project(id: Long): ProyectoEntity?
    suspend fun event(id: Long): EventoEntity?
    suspend fun enqueueProject(operation: CreationEntity, project: ProyectoEntity): Long
    suspend fun enqueueEvent(operation: CreationEntity, event: EventoEntity): Long
    suspend fun pending(): List<CreationEntity>
    suspend fun operation(id: Long): CreationEntity?
    suspend fun confirmProject(id: Long, project: ProyectoEntity)
    suspend fun confirmEvent(id: Long, event: EventoEntity)
    suspend fun error(operation: CreationEntity, permanent: Boolean, message: String)
}

class RoomCreationStore(private val dao: SyncPassDao) : CreationStore {
    override suspend fun profile() = dao.perfilLocal()
    override suspend fun project(id: Long) = dao.proyectoLocal(id)
    override suspend fun event(id: Long) = dao.eventoLocal(id)
    override suspend fun enqueueProject(operation: CreationEntity, project: ProyectoEntity) = dao.encolarProyecto(operation, project)
    override suspend fun enqueueEvent(operation: CreationEntity, event: EventoEntity) = dao.encolarEvento(operation, event)
    override suspend fun pending() = dao.creacionesPendientes()
    override suspend fun operation(id: Long) = dao.creacion(id)
    override suspend fun confirmProject(id: Long, project: ProyectoEntity) = dao.confirmarProyecto(id, project)
    override suspend fun confirmEvent(id: Long, event: EventoEntity) = dao.confirmarEvento(id, event)
    override suspend fun error(operation: CreationEntity, permanent: Boolean, message: String) = dao.errorCreacion(operation, permanent, message)
}
