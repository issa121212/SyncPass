package com.rojas.syncpass

import com.rojas.syncpass.data.local.RecintoDao
import com.rojas.syncpass.data.local.RecintoEntity
import com.rojas.syncpass.data.local.SyncPassDao
import com.rojas.syncpass.data.mapper.toEntity
import com.rojas.syncpass.data.remote.ProyectoResponse
import com.rojas.syncpass.data.remote.SyncPassApi
import com.rojas.syncpass.data.repository.ApiProyectosRepository
import com.rojas.syncpass.data.repository.RoomRecintosRepository
import com.rojas.syncpass.domain.AppException
import com.rojas.syncpass.domain.ErrorCode
import com.rojas.syncpass.domain.model.Recinto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import java.io.IOException

class DatosRepositoryTest {
    @Test
    fun queryingProjectCachesItsConfirmedName() = runBlocking {
        val api = mock(SyncPassApi::class.java)
        val dao = mock(SyncPassDao::class.java)
        `when`(dao.observarProyectos()).thenReturn(flowOf(emptyList()))
        val response = ProyectoResponse(id = 7, nombre = "Huerto")
        `when`(api.proyecto(7)).thenReturn(response)

        val proyecto = ApiProyectosRepository(api, dao).consultarProyecto(7).getOrThrow()

        assertEquals(7L, proyecto.id)
        assertEquals("Huerto", proyecto.nombre)
        verify(dao).actualizarProyecto(response.toEntity())
    }

    @Test
    fun networkFailurePreservesCachedProjectOnDeletion() = runBlocking {
        val api = mock(SyncPassApi::class.java)
        val dao = mock(SyncPassDao::class.java)
        `when`(dao.observarProyectos()).thenReturn(flowOf(emptyList()))
        `when`(api.eliminarProyecto(7)).thenAnswer { throw IOException("Sin conexión") }

        val error = ApiProyectosRepository(api, dao).eliminarProyecto(7).exceptionOrNull() as AppException

        assertEquals(ErrorCode.RED, error.codigo)
        assertTrue(error.reintentable)
        verify(dao, never()).eliminarProyecto(7)
    }

    @Test
    fun localVenueIsNormalizedAndReadsItsPersistedIdentity() = runBlocking {
        val dao = mock(RecintoDao::class.java)
        val stored = RecintoEntity(id = 9, nombre = "Biblioteca", direccion = "Calle Uno")
        `when`(dao.observar()).thenReturn(flowOf(listOf(stored)))
        val repository = RoomRecintosRepository(dao)

        repository.guardarRecinto(Recinto(nombre = " Biblioteca ", direccion = " Calle Uno ")).getOrThrow()

        verify(dao).crear(stored.copy(id = 0))
        assertEquals(Recinto(id = 9, nombre = "Biblioteca", direccion = "Calle Uno"), repository.recintos.first().single())
    }

    @Test
    fun updatingMissingVenueReturnsFailure() = runBlocking {
        val dao = mock(RecintoDao::class.java)
        `when`(dao.observar()).thenReturn(flowOf(emptyList()))
        val repository = RoomRecintosRepository(dao)

        assertTrue(repository.guardarRecinto(Recinto(id = 9, nombre = "Sede", direccion = "Calle Dos")).isFailure)
        assertTrue(repository.eliminarRecinto(9).isFailure)
    }
}
