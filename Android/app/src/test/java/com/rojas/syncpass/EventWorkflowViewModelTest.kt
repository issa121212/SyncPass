package com.rojas.syncpass

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.rojas.syncpass.data.mapper.*
import com.rojas.syncpass.data.remote.*
import com.rojas.syncpass.domain.SyncPassOperations
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.ui.event.EventoDetalleViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class EventWorkflowViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private class Fake : SyncPassOperations by mock(SyncPassOperations::class.java) {
        val event = MutableStateFlow<Evento?>(EventoResponse(id = 7,proyectoId = 1,nombre = "Taller",fechaHora = "2026-12-04T14:00:45",cupos = 10,inscritos = 2).toEntity().toDomain())
        override val perfil = MutableStateFlow<Perfil?>(Perfil(usuarioId = 3,nombre = "Ana",email = "",roles = "DOCENTE",qrContenido = ""))
        override val proyectos = MutableStateFlow(listOf(ProyectoResponse(id = 1,nombre = "Huerto",responsable = ResponsableResponse(id = 3)).toEntity().toDomain()))
        var latest = requireNotNull(event.value)
        var edited: EventoInput? = null
        var editCalls = 0
        var deleteCalls = 0
        var hold: CompletableDeferred<Unit>? = null
        var editFailure: Throwable? = null
        var registerFailure: Throwable? = null
        var survey: Triple<Int,Int,String?>? = null
        override fun observarEvento(id: Long) = event
        override suspend fun refrescarEvento(id: Long) = Result.success(latest)
        override suspend fun asistentes(eventoId: Long) = Result.success(listOf(Asistente(nombre = "Luis")))
        override suspend fun encuestas(eventoId: Long) = Result.success(listOf(EncuestaEvento(comentarios = "Bien")))
        override suspend fun editarEvento(id: Long,request: EventoInput): Result<Evento> {
            editCalls++; edited = request; hold?.await()
            return editFailure?.let { Result.failure(it) } ?: Result.success(latest.copy(estado = request.estado ?: "PROGRAMADO"))
        }
        override suspend fun eliminarEvento(id: Long): Result<Unit> { deleteCalls++; return Result.success(Unit) }
        override suspend fun inscribirse(evento: Evento) = registerFailure?.let { Result.failure<Unit>(it) } ?: Result.success(Unit)
        override suspend fun guardarEncuesta(eventoId: Long,satisfaccion: Int,utilidad: Int,comentarios: String?): Result<Unit> {
            survey = Triple(satisfaccion,utilidad,comentarios); return Result.success(Unit)
        }
    }

    @Test fun cancelarRecargaVersionActualYConservaFechaCuposYDemasesDatos() = runTest(main.dispatcher) {
        val repo = Fake()
        val store = ViewModelStore()
        val vm = EventoDetalleViewModel(SavedStateHandle(mapOf("id" to 7L)),repo).also { store.put("detail",it) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
            advanceUntilIdle()
            assertEquals("Luis",vm.uiState.value.asistentes.single().nombre)
            assertEquals("Bien",vm.uiState.value.encuestas.single().comentarios)
            repo.latest = repo.latest.copy(nombre = "Actualizado en servidor",cupos = 20)
            vm.cancelarEvento(); advanceUntilIdle()
            assertEquals("Actualizado en servidor",repo.edited?.nombre)
            assertEquals(20,repo.edited?.cupos)
            assertEquals("2026-12-04T14:00:45",repo.edited?.fechaHora)
            assertEquals("CANCELADO",repo.edited?.estado)
            assertEquals(1,vm.uiState.value.revisionGuardado)
            assertFalse(vm.uiState.value.guardando)
        } finally { store.clear() }
    }

    @Test fun cupoInsuficienteNoEscribeYErrorDeServidorMantieneEditorRecuperable() = runTest(main.dispatcher) {
        val repo = Fake(); val store = ViewModelStore()
        val vm = EventoDetalleViewModel(SavedStateHandle(mapOf("id" to 7L)),repo).also { store.put("detail",it) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }; advanceUntilIdle()
            vm.guardarEvento(repo.latest.toInput().copy(cupos = 1)); advanceUntilIdle()
            assertEquals(0,repo.editCalls)
            assertNotNull(vm.uiState.value.errorEdicion)
            vm.limpiarErrorEdicion(); runCurrent(); assertNull(vm.uiState.value.errorEdicion)
            repo.editFailure = IllegalStateException("Servidor no disponible")
            vm.guardarEvento(repo.latest.toInput()); advanceUntilIdle()
            assertEquals("Servidor no disponible",vm.uiState.value.errorEdicion)
            assertFalse(vm.uiState.value.guardando)
            assertEquals(0,vm.uiState.value.revisionGuardado)
        } finally { store.clear() }
    }

    @Test fun mutacionEnCursoImpideEditarOEliminarOtraVezYEliminarMarcaSalida() = runTest(main.dispatcher) {
        val repo = Fake(); val store = ViewModelStore()
        val vm = EventoDetalleViewModel(SavedStateHandle(mapOf("id" to 7L)),repo).also { store.put("detail",it) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }; advanceUntilIdle()
            repo.hold = CompletableDeferred()
            vm.guardarEvento(repo.latest.toInput()); runCurrent()
            vm.guardarEvento(repo.latest.toInput()); vm.eliminarEvento(); runCurrent()
            assertEquals(1,repo.editCalls)
            assertEquals(0,repo.deleteCalls)
            repo.hold?.complete(Unit); advanceUntilIdle()
            vm.eliminarEvento(); advanceUntilIdle()
            assertTrue(vm.uiState.value.eliminado)
            assertEquals(1,repo.deleteCalls)
        } finally { store.clear() }
    }

    @Test fun inscripcionYEncuestaComunicanResultadoYLimpianComentariosVacios() = runTest(main.dispatcher) {
        val repo = Fake(); repo.perfil.value = repo.perfil.value?.copy(roles = "PARTICIPANTE")
        val store = ViewModelStore()
        val vm = EventoDetalleViewModel(SavedStateHandle(mapOf("id" to 7L)),repo).also { store.put("detail",it) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }; advanceUntilIdle()
            vm.inscribirse(); advanceUntilIdle()
            assertEquals("Inscripción guardada",vm.uiState.value.mensaje)
            vm.limpiarMensaje(); runCurrent(); assertNull(vm.uiState.value.mensaje)
            repo.registerFailure = IllegalArgumentException("Sin cupos")
            vm.inscribirse(); advanceUntilIdle()
            assertTrue(vm.uiState.value.error.orEmpty().contains("Sin cupos"))
            vm.guardarEncuesta(5,4,"  "); advanceUntilIdle()
            assertEquals(Triple(5,4,null),repo.survey)
            assertEquals("Encuesta guardada",vm.uiState.value.mensaje)
            assertFalse(vm.uiState.value.cargando)
        } finally { store.clear() }
    }
}
