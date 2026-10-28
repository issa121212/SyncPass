package com.rojas.syncpass.data.repository
import com.rojas.syncpass.domain.RecintosRepository
import com.rojas.syncpass.domain.model.Recinto
import com.rojas.syncpass.data.local.RecintoDao
import com.rojas.syncpass.data.mapper.*
import com.rojas.syncpass.data.remote.resultado
import kotlinx.coroutines.flow.map
class RoomRecintosRepository(private val dao: RecintoDao) : RecintosRepository {
    override val recintos = dao.observar().map { list -> list.map { it.toDomain() } }
    override suspend fun guardarRecinto(item: Recinto): Result<Unit> = resultado {
        item.validar()
        val entity = item.copy(nombre = item.nombre.trim(), direccion = item.direccion.trim()).toEntity()
        if (item.id == 0L) dao.crear(entity) else check(dao.editar(entity) == 1) { "El recinto ya no existe" }
        Unit
    }
    override suspend fun eliminarRecinto(id: Long): Result<Unit> = resultado { check(dao.eliminar(id) == 1) }
}
