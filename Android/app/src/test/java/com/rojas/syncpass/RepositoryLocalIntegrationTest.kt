package com.rojas.syncpass

import android.app.Application
import androidx.room.Room
import com.rojas.syncpass.data.local.*
import com.rojas.syncpass.data.mapper.*
import com.rojas.syncpass.data.remote.*
import com.rojas.syncpass.data.repository.SyncPassRepository
import com.rojas.syncpass.data.session.SessionStore
import com.rojas.syncpass.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RepositoryLocalIntegrationTest {
    private lateinit var db: SyncPassDatabase
    private lateinit var repo: SyncPassRepository
    private val api = mock(SyncPassApi::class.java)
    private val session = mock(SessionStore::class.java)
    private val project = ProyectoResponse(id = 1, nombre = "Huerto", responsable = ResponsableResponse(id = 3))
    private val event = EventoResponse(id = 7, proyectoId = 1, nombre = "Taller", fechaHora = "2026-12-04T14:00:00")
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), SyncPassDatabase::class.java).build()
        `when`(session.cognitoName()).thenReturn("Ana Cognito")
        repo = SyncPassRepository(api,db.dao(),session,db.recintoDao())
        db.dao().guardarEventos(listOf(event.toEntity()))
        db.dao().guardarProyectos(listOf(project.toEntity()))
    }
    @After fun close() { db.close() }

    @Test fun refrescoParticipanteGuardaQrYNombreConfiableYUnFalloNoBorraCatalogo() = runBlocking {
        `when`(api.me()).thenReturn(UsuarioResponse(3,"sub","ana@example.invalid","UUID-backend",listOf("PARTICIPANTE")))
        `when`(api.miQr()).thenReturn(QrResponse("CREDENCIAL"))
        `when`(api.proyectos()).thenReturn(listOf(project))
        `when`(api.eventos(null)).thenReturn(listOf(event))
        repo.refrescar().getOrThrow()
        assertEquals("Ana Cognito", repo.perfil.first()?.nombre)
        assertEquals("CREDENCIAL", repo.perfil.first()?.qrContenido)
        assertEquals(7L, repo.eventos.first().single().id)
        `when`(api.me()).thenAnswer { throw IOException("Sin red") }
        assertTrue(repo.refrescar().isFailure)
        assertEquals("Huerto", repo.proyectos.first().single().nombre)
        assertEquals("Taller",repo.observarEvento(7).first()?.nombre)
    }

    @Test fun inscripcionDeRedQuedaPendientePeroConflictoNoSeEncolaYEncuestaSeConfirma() = runBlocking {
        `when`(api.inscribirse(InscribirseRequest("EVENTO_7"))).thenAnswer { throw IOException("Sin red") }
        repo.inscribirse(event.toEntity().toDomain()).getOrThrow()
        assertTrue(requireNotNull(db.dao().eventoLocal(7)).inscrito)
        val pending = db.dao().pendientes().single()
        assertEquals(PendienteEntity.TIPO_INSCRIPCION,pending.tipo)
        doReturn(event).`when`(api).inscribirse(InscribirseRequest("EVENTO_7"))
        assertEquals(1,repo.sincronizar().getOrThrow())
        assertTrue(db.dao().pendientes().isEmpty())
        doAnswer { throw HttpException(Response.error<Unit>(409,"{}".toResponseBody())) }.`when`(api).inscribirse(InscribirseRequest("EVENTO_7"))
        assertTrue(repo.inscribirse(event.toEntity().toDomain()).isFailure)
        assertTrue(db.dao().pendientes().isEmpty())
        `when`(api.responderEncuesta(anyLong(), (any(EncuestaRequest::class.java) ?: EncuestaRequest(1,1)))).thenReturn(EncuestaResponse(id = 9, satisfaccion = 5, utilidad = 4))
        repo.guardarEncuesta(7,5,4,"Útil").getOrThrow()
        assertTrue(requireNotNull(db.dao().eventoLocal(7)).encuestaRespondida)
        assertTrue(db.dao().pendientes().isEmpty())
        verify(api).responderEncuesta(eq(7L),argThat { it != null && it.satisfaccion == 5 && it.utilidad == 4 && it.comentarios == "Útil" && it.enviadoEn != null } ?: EncuestaRequest(1,1))
        Unit
    }

    @Test fun creacionesOfflineSeConsultanLocalmenteYSeConfirmanEnOrdenSinDuplicar() = runBlocking {
        db.dao().guardarPerfil(PerfilEntity(usuarioId = 3,nombre = "Ana",email = "ana@example.invalid",roles = "DOCENTE",qrContenido = ""))
        `when`(api.creationCapabilities()).thenAnswer { throw IOException("Sin red") }
        repo.crearProyecto(ProyectoInput("Proyecto sin conexión")).getOrThrow()
        val local = repo.proyectos.first().single { it.id < 0 }
        assertEquals("Proyecto sin conexión",repo.consultarProyecto(local.id).getOrThrow().nombre)
        val child = repo.crearEvento(EventoInput(local.id,"Taller offline",fechaHora = "2026-12-04T14:00:00")).getOrThrow()
        assertEquals("PENDIENTE",child.syncState)
        assertTrue(repo.refrescarEvento(child.id).isFailure)
        assertTrue(repo.inscribirse(child).isFailure)
        doReturn(CreationCapabilities(true)).`when`(api).creationCapabilities()
        `when`(api.crearProyecto((any(ProyectoRequest::class.java) ?: ProyectoRequest("")))).thenReturn(project.copy(id = 10,nombre = local.nombre,codigoPublico = "PRY-ABCDEF123456"))
        `when`(api.crearEvento((any(EventoRequest::class.java) ?: EventoRequest(1,"",fechaHora = "")))).thenReturn(event.copy(id = 20,proyectoId = 10,codigoPublico = "EVT-ABCDEF123456"))
        assertEquals(2,repo.sincronizar().getOrThrow())
        assertNull(db.dao().proyectoLocal(local.id))
        assertNull(db.dao().eventoLocal(child.id))
        assertEquals("EVT-ABCDEF123456",repo.observarEvento(20).first()?.codigoPublico)
        assertEquals(0,repo.pendientes.first())
    }

    @Test fun detalleAsistentesEncuestasYRechazosSeConviertenSinPerderDatos() = runBlocking {
        `when`(api.evento(7)).thenReturn(event.copy(nombre = "Confirmado"))
        assertEquals("Confirmado",repo.refrescarEvento(7).getOrThrow().nombre)
        `when`(api.asistentes(7)).thenReturn(listOf(AsistenteResponse(4,"Luis","luis@example.invalid",true,true,"2026-12-04T14:00:00Z",EncuestaResponse(9,5,4,"Bien"))))
        `when`(api.encuestas(7)).thenReturn(listOf(EncuestaEventoResponse(9,7,ResponsableResponse(4,"Luis"),5,4,"Bien")))
        assertEquals(5,repo.asistentes(7).getOrThrow().single().encuesta?.satisfaccion)
        assertEquals("Luis",repo.encuestas(7).getOrThrow().single().estudiante?.nombre)
        db.dao().encolar(PendienteEntity("rechazo",PendienteEntity.TIPO_INSCRIPCION,7,"EVENTO_7","2026-12-04T14:00:00Z"))
        db.dao().marcarRechazado("rechazo","Evento cancelado")
        assertEquals("Evento cancelado",repo.rechazadas.first().single().motivo)
        repo.cerrarSesionLocal()
        verify(session).clear()
    }
}
