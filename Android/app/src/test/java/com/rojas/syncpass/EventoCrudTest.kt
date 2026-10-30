package com.rojas.syncpass

import com.rojas.syncpass.data.local.*
import com.rojas.syncpass.data.remote.*
import com.rojas.syncpass.data.repository.SyncPassRepository
import com.rojas.syncpass.data.session.SessionStore
import com.rojas.syncpass.data.mapper.toRequest
import com.rojas.syncpass.data.mapper.toEntity
import com.rojas.syncpass.domain.model.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import java.io.IOException
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class EventoCrudTest {
    private val api = mock(SyncPassApi::class.java)
    private val dao = mock(SyncPassDao::class.java)
    private val input = EventoInput(1, "Taller", fechaHora = "2026-12-04T14:00:00")
    private fun repository(): SyncPassRepository {
        `when`(dao.observarPerfil()).thenReturn(flowOf(null))
        return SyncPassRepository(api, dao, mock(SessionStore::class.java), mock(RecintoDao::class.java))
    }

    @Test fun editingOnlyCachesTheConfirmedResponse() = runBlocking {
        val confirmed = EventoResponse(id = 7, proyectoId = 1, nombre = "Nombre confirmado", fechaHora = input.fechaHora)
        `when`(api.editarEvento(7, input.toRequest())).thenReturn(confirmed)
        val actual = repository().editarEvento(7, input).getOrThrow()
        assertEquals("Nombre confirmado", actual.nombre)
        verify(dao).guardarEventoConPendientes(confirmed.toEntity())
        Unit
    }
    @Test fun offlineEditingDoesNotChangeRoomOrQueueAWriting() = runBlocking {
        `when`(api.editarEvento(7, input.toRequest())).thenAnswer { throw IOException("offline") }
        assertTrue(repository().editarEvento(7, input).isFailure)
        assertFalse(mockingDetails(dao).invocations.any { it.method.name in listOf("guardarEventoConPendientes", "guardarPendiente", "guardarEvento") })
    }
    @Test fun deletionCallsServerBeforeRemovingTheLocalCopy() = runBlocking {
        `when`(dao.pendientesDelEvento(7)).thenReturn(0)
        repository().eliminarEvento(7).getOrThrow()
        val order = inOrder(api, dao)
        order.verify(api).eliminarEvento(7)
        order.verify(dao).eliminarEvento(7)
    }
    @Test fun conflictPreservesTheLocalEvent() = runBlocking {
        `when`(dao.pendientesDelEvento(7)).thenReturn(0)
        `when`(api.eliminarEvento(7)).thenAnswer { throw HttpException(Response.error<Unit>(409, "{}".toResponseBody())) }
        assertTrue(repository().eliminarEvento(7).isFailure)
        verify(dao, never()).eliminarEvento(7)
    }
    @Test fun pendingOperationsPreventDeletion() = runBlocking {
        `when`(dao.pendientesDelEvento(7)).thenReturn(1)
        assertTrue(repository().eliminarEvento(7).isFailure)
        verify(api, never()).eliminarEvento(7)
        verify(dao, never()).eliminarEvento(7)
    }
    @Test fun validationRejectsInvalidCoordinatesAndCapacity() {
        listOf(input.copy(latitud = 0.0), input.copy(cupos = -1), input.copy(radioMetros = 50001),
            input.copy(fechaHora = "2026-02-30T14:00"), input.copy(nombre = " "),
            input.copy(latitud = Double.NaN, longitud = 0.0)).forEach {
            assertTrue(runCatching { it.validar() }.isFailure)
        }
        input.copy(latitud = 0.0, longitud = 0.0, estado = "CANCELADO").validar()
    }
    @Test fun onlyOwnerTeacherOrAdminCanManageProject() {
        val project = Proyecto(1, "Proyecto", null, null, null, null, null, "PLANIFICADO", null, null, 8)
        val profile = Perfil(usuarioId = 8, nombre = "Ana", email = "", roles = "DOCENTE", qrContenido = "")
        assertTrue(puedeGestionarProyecto(project, profile))
        assertFalse(puedeGestionarProyecto(project, profile.copy(usuarioId = 9)))
        assertFalse(puedeGestionarProyecto(project, profile.copy(roles = "PARTICIPANTE")))
        assertTrue(puedeGestionarProyecto(project, profile.copy(usuarioId = 9, roles = "ADMIN")))
    }
}
