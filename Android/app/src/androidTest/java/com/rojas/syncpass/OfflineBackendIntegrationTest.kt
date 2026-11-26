package com.rojas.syncpass

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.cognitoauth.CognitoTokens
import com.rojas.syncpass.data.local.*
import com.rojas.syncpass.data.remote.*
import com.rojas.syncpass.data.repository.SyncPassRepository
import com.rojas.syncpass.data.session.SessionStore
import com.rojas.syncpass.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import retrofit2.HttpException
import java.io.IOException
import java.net.URL
import java.time.Instant
import java.util.UUID

/** Room y HTTP reales contra la copia local; nunca usa BuildConfig.API_BASE_URL ni tokens Cognito reales. */
class OfflineBackendIntegrationTest {
    private lateinit var context: Context
    private lateinit var session: SessionStore
    private lateinit var api: SyncPassApi
    private lateinit var participantToken: String
    private lateinit var name: String
    private lateinit var db: SyncPassDatabase
    private lateinit var repo: SyncPassRepository

    @Before fun setup() = runBlocking {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("syncpassRehearsal") == "true")
        context = ApplicationProvider.getApplicationContext()
        check(context.packageName.endsWith(".validation")) { "No ejecutar con la instalación personal" }
        WorkManager.getInstance(context).cancelAllWork().result.get()
        val connection = URL("http://127.0.0.1:18082/tokens").openConnection().apply { connectTimeout = 5000; readTimeout = 5000 }
        val tokens = JSONObject(connection.getInputStream().bufferedReader().use { it.readText() })
        session = SessionStore(context)
        session.save(CognitoTokens(accessToken = tokens.getString("teacher"), idToken = tokens.getString("teacher"), refreshToken = null, expiresIn = 3600))
        participantToken = tokens.getString("participant")
        api = ApiClient.crear("http://127.0.0.1:18081/", session, requireNotNull(session.accountKey()))
        name = "offline-integration-${UUID.randomUUID()}.db"
        reopen(api)
        repo.refrescar().getOrThrow()
    }

    private fun reopen(client: SyncPassApi) {
        db = Room.databaseBuilder(context, SyncPassDatabase::class.java, name).build()
        repo = SyncPassRepository(client, db.dao(), session, db.recintoDao())
    }
    @After fun cleanup() {
        if (::db.isInitialized) db.close()
        if (::context.isInitialized && ::name.isInitialized) context.deleteDatabase(name)
        if (::session.isInitialized) session.clear()
    }

    @Test fun creacionOfflinePersisteSeReabreYSincronizaPadreAntesDelEventoSinDuplicados() = runBlocking {
        val suffix = UUID.randomUUID().toString()
        val offline = object : SyncPassApi by api {
            override suspend fun creationCapabilities(): CreationCapabilities = throw IOException("Red del ensayo desconectada")
        }
        db.close(); reopen(offline)
        repo.crearProyecto(ProyectoInput("Proyecto offline $suffix", descripcion = "Descripción guardada en terreno")).getOrThrow()
        val localProject = db.dao().observarProyectos().first().single { it.id < 0 }
        val localEvent = repo.crearEvento(EventoInput(localProject.id, "Evento offline $suffix",
            descripcion = "Conservar fecha, hora y zona", fechaHora = "2026-11-25T14:30:15",
            cupos = 12, latitud = -33.45, longitud = -70.66, radioMetros = 200)).getOrThrow()
        val requests = db.dao().creacionesPendientes().associate { it.tipo to it.clientUuid }
        assertEquals(2, db.dao().observarPendientes().first())
        assertEquals("PENDIENTE", localEvent.syncState)
        assertNull(localProject.codigoPublico)
        assertNull(localEvent.codigoPublico)
        db.close(); reopen(api)
        assertEquals("2026-11-25T14:30:15", db.dao().eventoLocal(localEvent.id)?.fechaHora)
        assertEquals(requests, db.dao().creacionesPendientes().associate { it.tipo to it.clientUuid })
        var loseResponse = true
        val lost = object : SyncPassApi by api {
            override suspend fun crearProyecto(request: ProyectoRequest): ProyectoResponse {
                val response = api.crearProyecto(request)
                if (loseResponse) { loseResponse = false; throw IOException("Respuesta perdida después de guardar") }
                return response
            }
        }
        val losingRepo = SyncPassRepository(lost, db.dao(), session, db.recintoDao())
        assertTrue(losingRepo.sincronizar().isFailure)
        assertEquals(2, db.dao().creacionesPendientes().size)
        assertEquals(2, repo.sincronizar().getOrThrow())
        assertEquals(0, repo.sincronizar().getOrThrow())
        val projects = api.proyectos().filter { it.nombre == "Proyecto offline $suffix" }
        val events = api.eventos().filter { it.nombre == "Evento offline $suffix" }
        assertEquals(1, projects.size); assertEquals(1, events.size)
        assertEquals(projects.single().id, events.single().proyectoId)
        assertEquals(200, events.single().radioMetros)
        assertTrue(requireNotNull(projects.single().codigoPublico).matches(Regex("PRY-[0-9A-F]{12}")))
        assertTrue(requireNotNull(events.single().codigoPublico).matches(Regex("EVT-[0-9A-F]{12}")))
        assertEquals(projects.single().codigoPublico, db.dao().proyectoLocal(projects.single().id)?.codigoPublico)
        assertEquals(events.single().codigoPublico, db.dao().eventoLocal(events.single().id)?.codigoPublico)
        assertNull(db.dao().proyectoLocal(localProject.id))
        assertNull(db.dao().eventoLocal(localEvent.id))
        assertEquals("CONFIRMADO", db.dao().eventoLocal(events.single().id)?.syncState)
        assertEquals(0, db.dao().observarPendientes().first())
        assertEquals(requests["PROYECTO"], db.dao().creacion(-localProject.id)?.clientUuid)
        Unit
    }

    @Test fun dosCreacionesConcurrentesConMismoUuidDevuelvenElMismoRecursoYDatosDistintosDan409() = runBlocking {
        val request = ProyectoRequest("Concurrencia ${UUID.randomUUID()}", clientUuid = UUID.randomUUID().toString())
        val responses = coroutineScope { List(2) { async(Dispatchers.IO) { api.crearProyecto(request) } }.awaitAll() }
        assertEquals(responses.first().id, responses.last().id)
        assertNotNull(responses.first().codigoPublico)
        assertEquals(responses.first().codigoPublico, responses.last().codigoPublico)
        assertEquals(1, api.proyectos().count { it.nombre == request.nombre })
        try { api.crearProyecto(request.copy(nombre = "Otro cuerpo")); fail("El mismo UUID no admite otros datos") }
        catch (error: HttpException) { assertEquals(409, error.code()) }
    }

    @Test fun geocercaObligatoriaRechazaAusenciaYFueraDeZonaYConfirmaSoloElEventoCorrecto() = runBlocking {
        val teacher = requireNotNull(session.tokens.value)
        session.save(CognitoTokens(accessToken = participantToken, idToken = participantToken, refreshToken = null, expiresIn = 3600))
        val participantApi = ApiClient.crear("http://127.0.0.1:18081/", session, requireNotNull(session.accountKey()))
        val participant = participantApi.me()
        try { participantApi.crearProyecto(ProyectoRequest("No permitido")); fail("Participante no puede crear") }
        catch (error: HttpException) { assertEquals(403, error.code()) }
        session.save(teacher)
        val project = api.crearProyecto(ProyectoRequest("Recepción ${UUID.randomUUID()}", clientUuid = UUID.randomUUID().toString()))
        val event = api.crearEvento(EventoRequest(project.id, "Evento geográfico", fechaHora = "2026-11-25T14:30:00",
            latitud = -33.45, longitud = -70.66, radioMetros = 200, clientUuid = UUID.randomUUID().toString()))
        val qr = "SYNC_PASS:ASISTENCIA:${event.id}:${participant.id}"
        try { api.escanear(event.id, AsistenciaRequest(codigoQr = qr)); fail("La ubicación debe ser obligatoria") }
        catch (error: HttpException) { assertEquals(422, error.code()); assertTrue(error.response()?.errorBody()?.string().orEmpty().contains("GEO_DATOS_AUSENTES")) }
        val now = Instant.now()
        val valid = AsistenciaRequest(codigoQr = qr, marcadoEn = now.toString(), latitud = -33.45,
            longitud = -70.66, precisionMetros = 8.0, clientUuid = UUID.randomUUID().toString(), ubicacionEn = now.minusSeconds(1).toString())
        try { api.escanear(event.id, valid.copy(latitud = 0.0, longitud = 0.0)); fail("La zona debe comprobarse en servidor") }
        catch (error: HttpException) { assertEquals(422, error.code()); assertTrue(error.response()?.errorBody()?.string().orEmpty().contains("GEO_FUERA_DE_ZONA")) }
        assertTrue(api.escanear(event.id, valid).esPresente)
        assertEquals(1, api.asistentes(event.id).count { it.presente && it.usuarioId == participant.id })
        assertEquals(1, api.evento(event.id).presentes)
        assertTrue(api.escanear(event.id, valid).esPresente)
        assertEquals(1, api.evento(event.id).presentes)
    }
}
