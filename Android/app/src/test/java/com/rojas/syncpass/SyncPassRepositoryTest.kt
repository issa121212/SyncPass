package com.rojas.syncpass

import com.rojas.syncpass.data.local.*
import com.rojas.syncpass.data.remote.*
import com.rojas.syncpass.data.repository.SyncPassRepository
import com.rojas.syncpass.data.session.SessionStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import java.io.IOException

class SyncPassRepositoryTest {
    private val api = mock(SyncPassApi::class.java)
    private val dao = mock(SyncPassDao::class.java)
    private val repository: SyncPassRepository
        get() {
            `when`(dao.observarPerfil()).thenReturn(flowOf(null))
            return SyncPassRepository(api, dao, mock(SessionStore::class.java), mock(RecintoDao::class.java))
        }

    private fun pending(id: String, type: String) = PendienteEntity(
        clientUuid = id, tipo = type, eventoId = 7, codigoQr = "STUDENT",
        marcadoEn = "2026-10-25T12:00:00Z")

    @Test fun failureIsNotReportedAsSuccessfulSync() = runBlocking {
        val p = pending("a", PendienteEntity.TIPO_INSCRIPCION)
        `when`(dao.pendientes()).thenReturn(listOf(p))
        `when`(api.inscribirse(InscribirseRequest("STUDENT"))).thenAnswer { throw IOException("offline") }
        assertTrue(repository.sincronizar().isFailure)
        verify(dao).marcarError("a", "No se pudo conectar con el servidor")
        verify(dao, never()).marcarSincronizado("a")
        Unit
    }

    @Test fun legacySelfAttendanceIsNeverSent() = runBlocking {
        val p = pending("a", PendienteEntity.TIPO_AUSENCIA_PROPIA)
        `when`(dao.pendientes()).thenReturn(listOf(p))
        assertTrue(repository.sincronizar().isSuccess)
        verify(dao).marcarRechazado("a", "No se pudo completar la operacion")
        verifyNoInteractions(api)
    }

    @Test fun failedRegistrationBlocksDependentAttendance() = runBlocking {
        `when`(dao.pendientes()).thenReturn(listOf(
            pending("a", PendienteEntity.TIPO_INSCRIPCION),
            pending("b", PendienteEntity.TIPO_ASISTENCIA_PROPIA)))
        `when`(api.inscribirse(InscribirseRequest("STUDENT"))).thenAnswer { throw IOException("offline") }
        assertTrue(repository.sincronizar().isFailure)
        verify(dao, never()).marcarSincronizado("b")
        verify(api).inscribirse(InscribirseRequest("STUDENT"))
        verifyNoMoreInteractions(api)
    }

    @Test fun scanDoesNotMarkTeacherPresent() = runBlocking {
        `when`(dao.pendientes()).thenReturn(emptyList())
        `when`(dao.contarPendiente(anyString())).thenReturn(0)
        assertTrue(repository.registrarAsistenciaEscaneo(7, "STUDENT", null, null, null).getOrThrow())
        verify(dao, never()).marcarPresente(7)
        verify(dao, never()).actualizarPresencia(7, true)
    }

    @Test fun greetingUsesCognitoNameInsteadOfBackendIdentifier() = runBlocking {
        val session = mock(SessionStore::class.java)
        `when`(session.cognitoName()).thenReturn("Ana Rojas")
        `when`(dao.observarPerfil()).thenReturn(flowOf(PerfilEntity(
            usuarioId = 1, nombre = "cognito-uuid", email = "test@example.com",
            roles = "ESTUDIANTE", qrContenido = "ESTUDIANTE_1")))
        val repo = SyncPassRepository(api, dao, session, mock(RecintoDao::class.java))
        assertEquals("Ana Rojas", repo.perfil.first()?.nombre)
    }

    @Test fun missingCognitoNameDoesNotExposeBackendIdentifier() = runBlocking {
        val session = mock(SessionStore::class.java)
        `when`(session.cognitoName()).thenReturn(null)
        `when`(dao.observarPerfil()).thenReturn(flowOf(PerfilEntity(
            usuarioId = 1, nombre = "cognito-uuid", email = "test@example.com",
            roles = "ESTUDIANTE", qrContenido = "ESTUDIANTE_1")))
        val repo = SyncPassRepository(api, dao, session, mock(RecintoDao::class.java))
        assertEquals("", repo.perfil.first()?.nombre)
    }

    @Test fun offlineScanIsPendingRatherThanConfirmed() = runBlocking {
        val p = pending("a", PendienteEntity.TIPO_ASISTENCIA_ESCANEO)
        `when`(dao.pendientes()).thenReturn(listOf(p))
        `when`(dao.contarPendiente(anyString())).thenReturn(1)
        `when`(api.escanear(7, AsistenciaRequest(codigoQr = "STUDENT",
            marcadoEn = p.marcadoEn, clientUuid = "a"))).thenAnswer { throw IOException("offline") }
        assertFalse(repository.registrarAsistenciaEscaneo(7, "STUDENT", null, null, null).getOrThrow())
        verify(dao, never()).marcarSincronizado("a")
    }

    @Test fun projectUpdateRequiresApiConfirmation() = runBlocking {
        val request = ProyectoRequest("Editado", null, null, null, null, null)
        `when`(api.editarProyecto(7, request)).thenAnswer { throw IOException("offline") }
        assertTrue(repository.editarProyecto(7, com.rojas.syncpass.domain.model.ProyectoInput("Editado", null, null, null, null, null)).isFailure)
        assertFalse(mockingDetails(dao).invocations.any { it.method.name == "actualizarProyecto" })
    }

    @Test fun projectDeleteIsNotAppliedLocallyWhenApiRejectsIt() = runBlocking {
        `when`(api.eliminarProyecto(7)).thenAnswer { throw IOException("offline") }
        assertTrue(repository.eliminarProyecto(7).isFailure)
        verify(dao, never()).eliminarProyecto(7)
    }

    @Test fun permanentRejectionDoesNotPreventOtherEventsFromSynchronizing() = runBlocking {
        val rejected = pending("a", PendienteEntity.TIPO_INSCRIPCION)
        val other = pending("b", PendienteEntity.TIPO_INSCRIPCION).copy(eventoId = 8, codigoQr = "EVENTO_8")
        `when`(dao.pendientes()).thenReturn(listOf(rejected, other))
        `when`(api.inscribirse(InscribirseRequest("STUDENT"))).thenAnswer {
            throw retrofit2.HttpException(retrofit2.Response.error<Any>(403,
                okhttp3.ResponseBody.create(null, "forbidden")))
        }
        assertEquals(1, repository.sincronizar().getOrThrow())
        verify(dao).marcarRechazado("a", "No tienes permisos para esta operacion")
        verify(dao).marcarSincronizado("b")
    }
    @Test fun cancellationDoesNotBecomeQueuedFailure() = runBlocking {
        `when`(dao.pendientes()).thenReturn(listOf(pending("a", PendienteEntity.TIPO_INSCRIPCION)))
        `when`(api.inscribirse(InscribirseRequest("STUDENT"))).thenAnswer { throw kotlinx.coroutines.CancellationException() }
        try { repository.sincronizar(); fail("Cancellation must propagate") }
        catch (_: kotlinx.coroutines.CancellationException) { }
        verify(dao, never()).marcarSincronizado("a")
    }
}
