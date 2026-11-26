package com.rojas.syncpass

import androidx.lifecycle.ViewModelStore
import com.rojas.syncpass.domain.SyncPassOperations
import com.rojas.syncpass.domain.OperacionRechazada
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.ui.home.HomeViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class CreationViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private class Repository : SyncPassOperations by mock(SyncPassOperations::class.java) {
        override val perfil = MutableStateFlow<Perfil?>(null)
        override val proyectos = MutableStateFlow<List<Proyecto>>(emptyList())
        override val eventos = MutableStateFlow<List<Evento>>(emptyList())
        override val pendientes = MutableStateFlow(0)
        override val recintos = MutableStateFlow<List<Recinto>>(emptyList())
        override val rechazadas = MutableStateFlow<List<OperacionRechazada>>(emptyList())
        var projectCalls = 0
        var eventCalls = 0
        var request: EventoInput? = null
        var completion = CompletableDeferred<Result<Unit>>()
        override suspend fun refrescar() = Result.success(Unit)
        override suspend fun crearProyecto(request: ProyectoInput): Result<Unit> { projectCalls++; return completion.await() }
        override suspend fun crearEvento(request: EventoInput): Result<Evento> {
            eventCalls++; this.request = request
            return completion.await().map { mock(Evento::class.java) }
        }
    }
    private val repo = Repository()
    private val store = ViewModelStore()
    @After fun clear() { store.clear() }
    private fun project(vm: HomeViewModel) = vm.crearProyecto("Proyecto", "", "", "", "", "")
    private fun event(vm: HomeViewModel) = vm.crearEvento(-1, "Taller", "Descripción", "2026-11-25T14:30:00", 12, "Sede", "Dirección", -33.45, -70.66, 200)

    @Test fun bloqueaEnviosSimultaneosYPublicaEstadoDeGuardadoSinPrometerConfirmacion() = runTest(main.dispatcher) {
        val vm = HomeViewModel(repo).also { store.put("home", it) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        project(vm); project(vm); event(vm); runCurrent()
        assertTrue(vm.uiState.value.creando)
        assertEquals(1, repo.projectCalls)
        assertEquals(0, repo.eventCalls)
        repo.completion.complete(Result.success(Unit)); advanceUntilIdle()
        assertFalse(vm.uiState.value.creando)
        assertEquals("Proyecto guardado; revisa su estado de sincronización", vm.uiState.value.mensaje)
    }

    @Test fun errorLiberaLaProteccionYPermiteGuardarUnaSolicitudCorregida() = runTest(main.dispatcher) {
        val vm = HomeViewModel(repo).also { store.put("home", it) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        repo.completion.complete(Result.failure(IllegalArgumentException("Fecha inválida")))
        project(vm); advanceUntilIdle()
        assertFalse(vm.uiState.value.creando)
        assertTrue(vm.uiState.value.error.orEmpty().contains("Fecha inválida"))
        repo.completion = CompletableDeferred(Result.success(Unit))
        project(vm); advanceUntilIdle()
        assertEquals(2, repo.projectCalls)
        assertNull(vm.uiState.value.error)
    }

    @Test fun eventoConservaReferenciaLocalFechaHoraYZonaParaLaCola() = runTest(main.dispatcher) {
        val vm = HomeViewModel(repo).also { store.put("home", it) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        repo.completion.complete(Result.success(Unit))
        event(vm); advanceUntilIdle()
        assertEquals(EventoInput(-1, "Taller", "Descripción", "2026-11-25T14:30:00", 12,
            "Sede", "Dirección", -33.45, -70.66, 200), repo.request)
        assertEquals("Evento guardado; revisa su estado de sincronización", vm.uiState.value.mensaje)
    }
}
