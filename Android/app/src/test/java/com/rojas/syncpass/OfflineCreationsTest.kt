package com.rojas.syncpass

import com.rojas.syncpass.data.local.*
import com.rojas.syncpass.data.remote.*
import com.rojas.syncpass.data.repository.OfflineCreations
import com.rojas.syncpass.data.repository.SyncPassRepository
import com.rojas.syncpass.data.session.SessionStore
import com.rojas.syncpass.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Answers
import java.io.IOException
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.mockito.Mockito.`when`

class OfflineCreationsTest {
    private class MemoryStore : CreationStore {
        var user: PerfilEntity? = PerfilEntity(usuarioId = 7, nombre = "Ana", email = "ana@example.com", roles = "DOCENTE", qrContenido = "")
        val operations = linkedMapOf<Long, CreationEntity>()
        val projects = linkedMapOf<Long, ProyectoEntity>()
        val events = linkedMapOf<Long, EventoEntity>()
        var failConfirmation = false
        override suspend fun profile() = user
        override suspend fun project(id: Long) = projects[id]
        override suspend fun event(id: Long) = events[id]
        override suspend fun enqueueProject(operation: CreationEntity, project: ProyectoEntity): Long {
            val id = (operations.keys.maxOrNull() ?: 0) + 1
            operations[id] = operation.copy(id = id); projects[-id] = project.copy(id = -id); return id
        }
        override suspend fun enqueueEvent(operation: CreationEntity, event: EventoEntity): Long {
            val id = (operations.keys.maxOrNull() ?: 0) + 1
            operations[id] = operation.copy(id = id); events[-id] = event.copy(id = -id); return id
        }
        override suspend fun pending() = operations.values.filter { it.estado == "PENDIENTE" }
        override suspend fun operation(id: Long) = operations[id]
        override suspend fun confirmProject(id: Long, project: ProyectoEntity) {
            if (failConfirmation) throw IllegalStateException("Disco temporalmente no disponible")
            projects[project.id] = project; projects.remove(-id)
            events.replaceAll { _, value -> if (value.proyectoId == -id) value.copy(proyectoId = project.id, proyectoNombre = project.nombre) else value }
            operations.replaceAll { _, value -> if (value.proyectoId == -id) value.copy(proyectoId = project.id) else value }
            operations[id] = operations.getValue(id).copy(estado = "CONFIRMADO", remoteId = project.id)
        }
        override suspend fun confirmEvent(id: Long, event: EventoEntity) {
            events[event.id] = event; events.remove(-id)
            operations[id] = operations.getValue(id).copy(estado = "CONFIRMADO", remoteId = event.id)
        }
        override suspend fun error(operation: CreationEntity, permanent: Boolean, message: String) {
            val state = if (permanent) "RECHAZADO" else "PENDIENTE"
            operations[operation.id] = operation.copy(estado = state, error = message)
            projects[-operation.id]?.let { projects[-operation.id] = it.copy(syncState = state, syncError = message) }
            events[-operation.id]?.let { events[-operation.id] = it.copy(syncState = state, syncError = message) }
        }
    }

    private class Server {
        var supported = true
        var offline = false
        var loseResponse = false
        var rejectProject: String? = null
        var cancelled = false
        val calls = mutableListOf<String>()
        val projectRequests = mutableListOf<ProyectoRequest>()
        val eventRequests = mutableListOf<EventoRequest>()
        val savedProjects = linkedMapOf<String, ProyectoResponse>()
        val savedEvents = linkedMapOf<String, EventoResponse>()
        val api = mock(SyncPassApi::class.java) { call ->
            when (call.method.name) {
                "creationCapabilities" -> {
                    if (offline) throw IOException("Sin conexión")
                    if (!supported) throw HttpException(Response.error<Any>(404, "{}".toResponseBody()))
                    CreationCapabilities(true)
                }
                "crearProyecto" -> {
                    if (cancelled) throw CancellationException()
                    val request = call.getArgument<ProyectoRequest>(0)
                    calls += "PROYECTO"; projectRequests += request
                    if (rejectProject == request.nombre) throw HttpException(Response.error<Any>(403, "{}".toResponseBody()))
                    val result = savedProjects.getOrPut(requireNotNull(request.clientUuid)) {
                        ProyectoResponse(id = 100L + savedProjects.size, nombre = request.nombre,
                            responsable = ResponsableResponse(7, "Ana", "ana@example.com"), codigoPublico = "PRY-ABCDEF123456")
                    }
                    if (loseResponse) { loseResponse = false; throw IOException("Respuesta perdida") }
                    result
                }
                "crearEvento" -> {
                    val request = call.getArgument<EventoRequest>(0)
                    calls += "EVENTO"; eventRequests += request
                    savedEvents.getOrPut(requireNotNull(request.clientUuid)) {
                        EventoResponse(id = 200L + savedEvents.size, proyectoId = request.proyectoId,
                            proyectoNombre = "Proyecto", nombre = request.nombre, fechaHora = request.fechaHora, codigoPublico = "EVT-123456ABCDEF")
                    }
                }
                else -> Answers.RETURNS_DEFAULTS.answer(call)
            }
        }
    }
    private val store = MemoryStore()
    private val server = Server()
    private val repo get() = OfflineCreations(server.api, store)
    private fun event(project: Long) = EventoInput(project, "Taller", fechaHora = "2026-11-25T12:00:00", cupos = 10,
        latitud = -33.45, longitud = -70.66, radioMetros = 200)

    @Test fun sinConexionGuardaProyectoYEventoDependienteYLosRecuperaOtroRepositorio() = runBlocking {
        server.offline = true
        val projectId = repo.project(ProyectoInput("Proyecto", descripcion = "Descripción"))
        val eventId = repo.event(event(-projectId))
        val result = repo.synchronize()
        assertEquals(0, result.first)
        assertNotNull(result.second)
        assertEquals("PENDIENTE", store.projects.getValue(-projectId).syncState)
        assertEquals(-projectId, store.events.getValue(-eventId).proyectoId)
        assertEquals("Taller", repo.eventResult(eventId).nombre)
        assertTrue(server.calls.isEmpty())
        assertEquals(2, store.pending().size)
    }

    @Test fun confirmaPrimeroProyectoLuegoEventoYReasignaLosIdentificadores() = runBlocking {
        val projectId = repo.project(ProyectoInput("Proyecto"))
        val eventId = repo.event(event(-projectId))
        assertNull(store.projects.getValue(-projectId).codigoPublico)
        assertNull(store.events.getValue(-eventId).codigoPublico)
        assertEquals(2, repo.synchronize().first)
        assertEquals("PRY-ABCDEF123456", store.projects.getValue(100L).codigoPublico)
        assertEquals("EVT-123456ABCDEF", repo.eventResult(eventId).codigoPublico)
        assertEquals(listOf("PROYECTO", "EVENTO"), server.calls)
        assertEquals(100L, server.eventRequests.single().proyectoId)
        assertFalse(store.projects.containsKey(-projectId))
        assertFalse(store.events.containsKey(-eventId))
        assertEquals(100L, repo.eventResult(eventId).proyectoId)
        assertEquals("CONFIRMADO", repo.eventResult(eventId).syncState)
        assertTrue(store.pending().isEmpty())
    }

    @Test fun respuestaPerdidaMantieneUuidYPayloadYNoEnviaAntesElHijo() = runBlocking {
        val projectId = repo.project(ProyectoInput("Proyecto"))
        repo.event(event(-projectId))
        server.loseResponse = true
        assertEquals(0, repo.synchronize().first)
        assertTrue(server.eventRequests.isEmpty())
        assertEquals(2, repo.synchronize().first)
        assertEquals(2, server.projectRequests.size)
        assertEquals(server.projectRequests.first(), server.projectRequests.last())
        assertEquals(1, server.savedProjects.size)
        assertEquals(1, server.savedEvents.size)
        assertEquals(server.savedProjects.values.single().codigoPublico, store.projects.getValue(100L).codigoPublico)
        assertEquals(server.savedEvents.values.single().codigoPublico, store.events.getValue(200L).codigoPublico)
    }

    @Test fun falloAlConfirmarEnLocalSeRecuperaConElMismoUuid() = runBlocking {
        repo.project(ProyectoInput("Proyecto"))
        store.failConfirmation = true
        assertNotNull(repo.synchronize().second)
        assertEquals("PENDIENTE", store.operations.values.single().estado)
        store.failConfirmation = false
        assertEquals(1, repo.synchronize().first)
        assertEquals(1, server.savedProjects.size)
        assertEquals(server.projectRequests.first(), server.projectRequests.last())
    }

    @Test fun rechazoDelPadreConservaDatosYRechazaElHijoSinEnviarlo() = runBlocking {
        server.rejectProject = "Rechazado"
        val projectId = repo.project(ProyectoInput("Rechazado", descripcion = "No perder"))
        val eventId = repo.event(event(-projectId))
        assertEquals(0, repo.synchronize().first)
        assertEquals("RECHAZADO", store.operations.getValue(projectId).estado)
        assertEquals("RECHAZADO", store.operations.getValue(eventId).estado)
        assertTrue(server.eventRequests.isEmpty())
        assertEquals("No perder", store.projects.getValue(-projectId).descripcion)
        assertTrue(runCatching { repo.projectResult(projectId) }.isFailure)
        assertEquals(0, repo.synchronize().first)
        assertEquals(1, server.projectRequests.size)
    }

    @Test fun backendAnteriorNoRecibeNingunPostYLaColaPermanecePendiente() = runBlocking {
        server.supported = false
        repo.project(ProyectoInput("Proyecto"))
        assertNotNull(repo.synchronize().second)
        assertEquals("PENDIENTE", store.operations.values.single().estado)
        assertTrue(server.calls.isEmpty())
        server.supported = true
        assertEquals(1, repo.synchronize().first)
    }

    @Test fun rechazoDeUnProyectoNoBloqueaCreacionesIndependientes() = runBlocking {
        server.rejectProject = "Rechazado"
        repo.project(ProyectoInput("Rechazado"))
        val good = repo.project(ProyectoInput("Permitido"))
        repo.event(event(-good))
        assertEquals(2, repo.synchronize().first)
        assertEquals(1, server.savedProjects.size)
        assertEquals(1, server.savedEvents.size)
    }

    @Test fun formularioConIdLocalAntiguoResuelveElProyectoYaConfirmado() = runBlocking {
        val projectId = repo.project(ProyectoInput("Proyecto"))
        repo.synchronize()
        val eventId = repo.event(event(-projectId))
        assertEquals(100L, store.events.getValue(-eventId).proyectoId)
        assertEquals(1, repo.synchronize().first)
        assertEquals(100L, server.eventRequests.single().proyectoId)
    }

    @Test fun cuentaSinPerfilODocenteAjenoNoPuedeEncolarCreaciones() = runBlocking {
        store.user = null
        assertTrue(runCatching { repo.project(ProyectoInput("Proyecto")) }.isFailure)
        store.user = PerfilEntity(usuarioId = 8, nombre = "Otra", email = "otra@example.com", roles = "PARTICIPANTE", qrContenido = "")
        assertTrue(runCatching { repo.project(ProyectoInput("Proyecto")) }.isFailure)
        store.user = store.user?.copy(roles = "DOCENTE")
        store.projects[10] = ProyectoEntity(10, "Ajeno", null, null, null, null, null, "PLANIFICADO", "Ana", null, 7)
        assertTrue(runCatching { repo.event(event(10)) }.isFailure)
        assertTrue(store.operations.isEmpty())
    }

    @Test fun cancelacionConservaLaCreacionPendienteYNoSeConvierteEnRechazo() = runBlocking {
        repo.project(ProyectoInput("Proyecto"))
        server.cancelled = true
        try { repo.synchronize(); fail("Debe propagarse la cancelación") } catch (_: CancellationException) { }
        assertEquals("PENDIENTE", store.operations.values.single().estado)
        server.cancelled = false
        assertEquals(1, repo.synchronize().first)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun sincronizacionManualYDeFondoCompartenLaExclusionYNoDuplicanEnvios() = runTest {
        repo.project(ProyectoInput("Proyecto"))
        val gate = CompletableDeferred<Unit>()
        var started = 0
        val delayedApi = object : SyncPassApi by server.api {
            override suspend fun crearProyecto(request: ProyectoRequest): ProyectoResponse {
                started++
                gate.await()
                return server.api.crearProyecto(request)
            }
        }
        val dao = mock(SyncPassDao::class.java)
        `when`(dao.observarEventos()).thenReturn(flowOf(emptyList()))
        `when`(dao.observarPerfil()).thenReturn(flowOf(null))
        `when`(dao.observarRechazadas()).thenReturn(flowOf(emptyList()))
        `when`(dao.observarPendientes()).thenReturn(flowOf(1))
        `when`(dao.pendientes()).thenReturn(emptyList())
        val coordinator = SyncPassRepository(delayedApi, dao, mock(SessionStore::class.java), mock(RecintoDao::class.java), store)
        val manual = async { coordinator.sincronizar().getOrThrow() }
        val worker = async { coordinator.sincronizar().getOrThrow() }
        runCurrent()
        assertEquals(1, started)
        gate.complete(Unit)
        assertEquals(1, manual.await())
        assertEquals(0, worker.await())
        assertEquals(1, server.projectRequests.size)
    }

    @Test fun datosInvalidosNoEntranEnLaColaYUnProyectoRechazadoNoAdmiteNuevosHijos() = runBlocking {
        assertTrue(runCatching { repo.project(ProyectoInput(" ")) }.isFailure)
        assertTrue(runCatching { repo.project(ProyectoInput("Proyecto", fechaInicio = "2026-12-01", fechaFin = "2026-11-01")) }.isFailure)
        assertTrue(store.operations.isEmpty())
        val projectId = repo.project(ProyectoInput("Rechazado"))
        server.rejectProject = "Rechazado"
        repo.synchronize()
        assertTrue(runCatching { repo.event(event(-projectId)) }.isFailure)
        assertEquals(1, store.operations.size)
    }
}
