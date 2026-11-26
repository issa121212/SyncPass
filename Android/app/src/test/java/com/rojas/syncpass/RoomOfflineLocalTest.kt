package com.rojas.syncpass

import android.app.Application
import androidx.room.Room
import com.rojas.syncpass.data.local.*
import com.rojas.syncpass.data.mapper.toEntity
import com.rojas.syncpass.data.remote.EventoResponse
import com.rojas.syncpass.data.remote.ProyectoResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

/** Componentes locales en JVM: Android simulado, SQLite aislado y sin red. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomOfflineLocalTest {
    private lateinit var db: SyncPassDatabase
    private lateinit var name: String
    private val context get() = RuntimeEnvironment.getApplication()
    private val dao get() = db.dao()
    private fun project(id: Long, title: String = "Proyecto") = ProyectoResponse(id = id, nombre = title).toEntity()
    private fun event(id: Long, parent: Long = 7) = EventoResponse(id = id, proyectoId = parent,
        nombre = "Taller", proyectoNombre = "Proyecto", fechaHora = "2026-12-04T14:30:00").toEntity()
    private fun operation(type: String, parent: Long? = null) = CreationEntity(
        clientUuid = UUID.randomUUID().toString(), tipo = type, payload = "{\"nombre\":\"Conservar\"}", proyectoId = parent)
    private fun pending(uuid: String, type: String) = PendienteEntity(uuid, type, 9, "EVENTO_9", "2026-12-04T14:30:00Z")
    private fun open() = Room.databaseBuilder(context, SyncPassDatabase::class.java, name).build()

    @Before fun setup() { name = "r-${UUID.randomUUID().toString().take(8)}.db"; db = open() }
    @After fun cleanup() { db.close(); context.deleteDatabase(name) }

    @Test fun confirmacionPadreHijoConservaPayloadYCodigosTrasReabrirSinDuplicarTarjetas() = runBlocking {
        dao.guardarPerfil(PerfilEntity(usuarioId = 3, nombre = "Ana", email = "ana@example.invalid", roles = "DOCENTE", qrContenido = ""))
        val store = RoomCreationStore(dao)
        val parent = store.enqueueProject(operation("PROYECTO"), project(0).copy(syncState = "PENDIENTE"))
        val child = store.enqueueEvent(operation("EVENTO", -parent), event(0, -parent).copy(syncState = "PENDIENTE"))
        val payload = requireNotNull(store.operation(child)).payload
        assertEquals(2, dao.observarPendientes().first())
        assertEquals(3L, store.profile()?.usuarioId)
        assertEquals(-parent, store.event(-child)?.proyectoId)
        store.confirmProject(parent, project(70, "Nombre confirmado").copy(codigoPublico = "PRY-ABCDEF123456"))
        assertNull(store.project(-parent))
        assertEquals(70L, store.event(-child)?.proyectoId)
        assertEquals("Nombre confirmado", store.event(-child)?.proyectoNombre)
        assertEquals(70L, store.operation(child)?.proyectoId)
        assertEquals(payload, store.operation(child)?.payload)
        store.confirmEvent(child, event(90, 70).copy(codigoPublico = "EVT-ABCDEF123456"))
        db.close(); db = open()
        assertEquals(listOf(70L), dao.observarProyectos().first().map { it.id })
        assertEquals(listOf(90L), dao.observarEventos().first().map { it.id })
        assertEquals("PRY-ABCDEF123456", dao.proyectoLocal(70)?.codigoPublico)
        assertEquals("EVT-ABCDEF123456", dao.observarEvento(90).first()?.codigoPublico)
        assertNull(dao.eventoLocal(-child))
        assertEquals(0, dao.observarPendientes().first())
        assertEquals("CONFIRMADO", dao.creacion(child)?.estado)
    }

    @Test fun refrescarCatalogoConservaCreacionesLocalesYReaplicaInscripcionYEncuestaPendientes() = runBlocking {
        dao.guardarProyectos(listOf(project(7, "Anterior")))
        dao.guardarEventos(listOf(event(9), event(10)))
        val local = dao.encolarProyecto(operation("PROYECTO"), project(0).copy(syncState = "PENDIENTE"))
        val localEvent = dao.encolarEvento(operation("EVENTO", -local), event(0, -local).copy(syncState = "PENDIENTE"))
        dao.encolar(pending("inscripcion", PendienteEntity.TIPO_INSCRIPCION))
        dao.encolar(pending("encuesta", PendienteEntity.TIPO_ENCUESTA))
        dao.reemplazarProyectos(listOf(project(7, "Actualizado")))
        dao.reemplazarEventos(listOf(event(9)))
        assertNull(dao.eventoLocal(10))
        assertNotNull(dao.proyectoLocal(-local))
        assertNotNull(dao.eventoLocal(-localEvent))
        assertTrue(requireNotNull(dao.eventoLocal(9)).inscrito)
        assertTrue(requireNotNull(dao.eventoLocal(9)).encuestaRespondida)
        assertFalse(requireNotNull(dao.eventoLocal(9)).esPresente)
        dao.guardarEventoConPendientes(event(9).copy(nombre = "Revisión remota"))
        assertTrue(requireNotNull(dao.eventoLocal(9)).inscrito)
        assertEquals(4, dao.observarPendientes().first())
        dao.actualizarProyecto(project(7, "Nombre nuevo"))
        assertEquals("Nombre nuevo", dao.eventoLocal(9)?.proyectoNombre)
        assertEquals("PENDIENTE", dao.eventoLocal(-localEvent)?.syncState)
    }

    @Test fun erroresTransitoriosYRechazosConservanDatosSinReenviarOperacionesDefinitivas() = runBlocking {
        val store = RoomCreationStore(dao)
        val parent = store.enqueueProject(operation("PROYECTO"), project(0).copy(syncState = "PENDIENTE"))
        val child = store.enqueueEvent(operation("EVENTO", -parent), event(0, -parent).copy(syncState = "PENDIENTE"))
        val original = requireNotNull(store.operation(parent))
        store.error(original, false, "Sin red")
        assertEquals(2, store.pending().size)
        assertEquals("Sin red", store.project(-parent)?.syncError)
        store.error(requireNotNull(store.operation(parent)), true, "Permiso insuficiente")
        store.error(requireNotNull(store.operation(child)), true, "Proyecto rechazado")
        assertTrue(store.pending().isEmpty())
        assertEquals("RECHAZADO", store.project(-parent)?.syncState)
        assertEquals("Proyecto rechazado", store.event(-child)?.syncError)
        assertEquals(original.payload, store.operation(parent)?.payload)
        assertEquals(0, dao.observarPendientes().first())
        dao.guardarEvento(event(9))
        dao.encolar(pending("captura", PendienteEntity.TIPO_ASISTENCIA_ESCANEO))
        dao.marcarError("captura", "Servidor temporalmente no disponible")
        assertEquals(1, dao.pendientesDelEvento(9))
        dao.marcarRechazado("captura", "Fuera de zona")
        assertEquals("Fuera de zona", dao.observarRechazadas().first().single().error)
        assertTrue(dao.pendientes().isEmpty())
        assertNotNull(dao.operacion("captura"))
    }

    @Test fun falloDeUnUuidDuplicadoNoReemplazaDatosNiGeneraUnaTarjetaAdicional() = runBlocking {
        val request = operation("PROYECTO")
        val id = dao.encolarProyecto(request, project(0, "Original").copy(syncState = "PENDIENTE"))
        val result = runCatching { dao.encolarProyecto(request, project(0, "Duplicado").copy(syncState = "PENDIENTE")) }
        assertTrue(result.isFailure)
        assertEquals(listOf("Original"), dao.observarProyectos().first().map { it.nombre })
        assertEquals(listOf(id), dao.creacionesPendientes().map { it.id })
        assertEquals(1, dao.observarPendientes().first())
    }

    @Test fun confirmarPendientesLimpiaSoloSincronizadosYNoInventaAsistenciaPersonal() = runBlocking {
        dao.guardarEvento(event(9))
        dao.encolar(pending("listo", PendienteEntity.TIPO_INSCRIPCION))
        dao.encolar(pending("por-enviar", PendienteEntity.TIPO_ENCUESTA))
        dao.marcarError("listo", "Reintentando")
        dao.marcarSincronizado("listo")
        assertEquals(0, dao.contarPendiente("listo"))
        assertNull(dao.operacion("listo")?.error)
        dao.limpiarSincronizados()
        assertNull(dao.operacion("listo"))
        assertEquals(listOf("por-enviar"), dao.pendientes().map { it.clientUuid })
        assertFalse(requireNotNull(dao.eventoLocal(9)).esPresente)
        // Si el servidor ya no publica el evento, su captura pendiente no debe perderse.
        dao.reemplazarEventos(emptyList())
        assertNull(dao.observarEvento(9).first())
        assertNotNull(dao.operacion("por-enviar"))
    }
}
