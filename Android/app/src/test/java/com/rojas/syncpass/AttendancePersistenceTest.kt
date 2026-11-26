package com.rojas.syncpass

import com.rojas.syncpass.data.local.*
import com.rojas.syncpass.data.remote.*
import com.rojas.syncpass.data.repository.*
import com.rojas.syncpass.data.session.SessionStore
import com.rojas.syncpass.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.IOException
import java.time.Instant

class AttendancePersistenceTest {
    private class MemoryDao : SyncPassDao by mock(SyncPassDao::class.java) {
        val rows = linkedMapOf<String, PendienteEntity>()
        override fun observarEventos() = flowOf(emptyList<EventoEntity>())
        override fun observarPerfil() = flowOf<PerfilEntity?>(null)
        override fun observarPendientes() = flowOf(0)
        override fun observarRechazadas() = flowOf(emptyList<PendienteEntity>())
        override suspend fun encolar(item: PendienteEntity) { rows[item.clientUuid] = item }
        override suspend fun operacion(uuid: String) = rows[uuid]
        override suspend fun pendientes() = rows.values.filter { !it.sincronizado && !it.rechazado }
        override suspend fun creacionesPendientes() = emptyList<CreationEntity>()
        override suspend fun marcarError(uuid: String, error: String) { rows[uuid] = rows.getValue(uuid).copy(error = error) }
        override suspend fun marcarRechazado(uuid: String, error: String) { rows[uuid] = rows.getValue(uuid).copy(rechazado = true, error = error) }
        override suspend fun marcarSincronizado(uuid: String) { rows[uuid] = rows.getValue(uuid).copy(sincronizado = true) }
        override suspend fun limpiarSincronizados() { rows.entries.removeAll { it.value.sincronizado } }
    }
    private val capture = AttendanceCapture(Instant.parse("2026-11-25T12:00:00Z"),
        DeviceLocation(GeoPoint(-33.45, -70.66), 9.0, Instant.parse("2026-11-25T11:59:58Z").toEpochMilli()))

    @Test fun perdidaDeRespuestaReenviaExactamenteElMismoUuidPosicionYHoras() = runBlocking {
        val dao = MemoryDao()
        val sent = mutableListOf<AsistenciaRequest>()
        val api = mock(SyncPassApi::class.java) { call ->
            if (call.method.name == "escanear") {
                sent += call.getArgument<AsistenciaRequest>(1)
                if (sent.size == 1) throw IOException("Respuesta perdida")
                AsistenciaResponse(esPresente = true)
            } else org.mockito.Answers.RETURNS_DEFAULTS.answer(call)
        }
        val repo = SyncPassRepository(api, dao, mock(SessionStore::class.java), mock(RecintoDao::class.java))
        assertEquals(AttendanceOutcome.Pending, repo.registrarAsistenciaEscaneo(7, "PARTICIPANT", capture).getOrThrow())
        val saved = dao.rows.values.single()
        assertEquals("2026-11-25T11:59:58Z", saved.ubicacionEn)
        assertEquals("2026-11-25T12:00:00Z", saved.marcadoEn)
        assertEquals(1, repo.sincronizar().getOrThrow())
        assertEquals(2, sent.size)
        assertEquals(sent.first(), sent.last())
        assertEquals(saved.clientUuid, sent.last().clientUuid)
        assertTrue(dao.rows.isEmpty())
    }

    @Test fun rechazoGeograficoPermaneceVisibleYNoSeReintentaIndefinidamente() = runBlocking {
        val dao = MemoryDao()
        var requests = 0
        val api = mock(SyncPassApi::class.java) { call ->
            if (call.method.name == "escanear") {
                requests++
                throw retrofit2.HttpException(retrofit2.Response.error<Any>(422,
                    okhttp3.ResponseBody.create(null, "{\"error\":\"UNPROCESSABLE_ENTITY\",\"mensaje\":\"GEO_FUERA_DE_ZONA\",\"status\":422,\"detalles\":{}}")))
            } else org.mockito.Answers.RETURNS_DEFAULTS.answer(call)
        }
        val repo = SyncPassRepository(api, dao, mock(SessionStore::class.java), mock(RecintoDao::class.java))
        val outcome = repo.registrarAsistenciaEscaneo(7, "PARTICIPANT", capture).getOrThrow()
        assertEquals(AttendanceOutcome.Rejected("La captura se realizó fuera de la zona permitida"), outcome)
        assertTrue(dao.rows.values.single().rechazado)
        assertEquals(0, repo.sincronizar().getOrThrow())
        assertEquals(1, requests)
        assertEquals(capture.location?.point?.latitude, dao.rows.values.single().latitud)
    }

    @Test fun cancelarDespuesDePersistirConservaLaCapturaParaElSiguienteIntento() = runBlocking {
        val dao = MemoryDao()
        val repo = RoomAsistenciasRepository(dao) { throw CancellationException("Sesión cerrada") }
        try { repo.capture(7, "PARTICIPANT", capture); fail("La cancelación debe propagarse") }
        catch (_: CancellationException) { }
        val saved = dao.rows.values.single()
        assertFalse(saved.rechazado)
        assertEquals(capture.capturedAt.toString(), saved.marcadoEn)
        assertEquals(9.0, saved.precisionMetros)
    }
}
