package com.rojas.syncpass.data.repository
import com.rojas.syncpass.domain.ProyectosRepository
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.data.local.SyncPassDao
import com.rojas.syncpass.data.remote.*
import com.rojas.syncpass.data.mapper.*
import kotlinx.coroutines.flow.map
class ApiProyectosRepository(private val api: SyncPassApi, private val dao: SyncPassDao) : ProyectosRepository {
    override val proyectos = dao.observarProyectos().map { list -> list.map { it.toDomain() } }
    override suspend fun consultarProyecto(id: Long): Result<Proyecto> = resultado {
        val entity = api.proyecto(id).toEntity(); dao.actualizarProyecto(entity); entity.toDomain()
    }
    override suspend fun crearProyecto(request: ProyectoInput): Result<Unit> = resultado { dao.actualizarProyecto(api.crearProyecto(request.toRequest()).toEntity()) }
    override suspend fun editarProyecto(id: Long, request: ProyectoInput): Result<Unit> = resultado { dao.actualizarProyecto(api.editarProyecto(id, request.toRequest()).toEntity()) }
    override suspend fun eliminarProyecto(id: Long): Result<Unit> = resultado { api.eliminarProyecto(id); dao.eliminarProyecto(id) }
}
