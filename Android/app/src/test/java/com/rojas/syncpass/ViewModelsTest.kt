package com.rojas.syncpass

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.rojas.syncpass.domain.AppException
import com.rojas.syncpass.domain.ErrorCode
import com.rojas.syncpass.domain.OperacionRechazada
import com.rojas.syncpass.domain.SyncPassOperations
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.ui.event.EventoDetalleViewModel
import com.rojas.syncpass.ui.gestion.GestionViewModel
import com.rojas.syncpass.ui.home.HomeViewModel
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
class ViewModelsTest {
    @get:Rule val main = MainDispatcherRule()

    private class FakeRepository : SyncPassOperations by mock(SyncPassOperations::class.java) {
        override val perfil = MutableStateFlow<Perfil?>(null)
        override val proyectos = MutableStateFlow<List<Proyecto>>(emptyList())
        override val eventos = MutableStateFlow<List<Evento>>(emptyList())
        override val pendientes = MutableStateFlow(0)
        override val rechazadas = MutableStateFlow<List<OperacionRechazada>>(emptyList())
        override val recintos = MutableStateFlow<List<Recinto>>(emptyList())
        var refreshResult: Result<Unit> = Result.success(Unit)
        var syncResult: Result<Int> = Result.success(0)
        var venueResult = CompletableDeferred<Result<Unit>>()
        var venueWrites = 0
        var consultedEvent: Long? = null
        val event = MutableStateFlow<Evento?>(null)

        override suspend fun refrescar() = refreshResult
        override suspend fun sincronizar() = syncResult
        override fun observarEvento(id: Long) = event
        override suspend fun refrescarEvento(id: Long): Result<Evento> {
            consultedEvent = id
            return Result.failure(AppException(ErrorCode.NO_EXISTE, false, "Evento no disponible"))
        }
        override suspend fun guardarRecinto(item: Recinto): Result<Unit> {
            venueWrites++
            return venueResult.await()
        }
    }

    @Test
    fun inicioConErrorMantieneElPerfilCacheadoYComunicaLaFaltaDeConexion() = runTest(main.dispatcher) {
        val repo = FakeRepository().apply {
            perfil.value = Perfil(usuarioId = 4, nombre = "Ana", email = "ana@example.com", roles = "DOCENTE", qrContenido = "")
            refreshResult = Result.failure(AppException(ErrorCode.RED, true, "Sin conexión"))
        }
        val vm = HomeViewModel(repo)
        val store = ViewModelStore().apply { put("inicio", vm) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
            advanceUntilIdle()
            assertEquals("Ana", vm.uiState.value.perfil?.nombre)
            assertTrue(vm.uiState.value.esDocente)
            assertTrue(vm.uiState.value.error.orEmpty().contains("Sin conexión"))
            assertFalse(vm.uiState.value.cargando)
        } finally { store.clear() }
    }

    @Test
    fun inicioInformaElResultadoDeSincronizarYConservaLosPendientesSiFalla() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        val vm = HomeViewModel(repo)
        val store = ViewModelStore().apply { put("inicio", vm) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
            advanceUntilIdle()
            repo.syncResult = Result.success(2)
            vm.sincronizar()
            advanceUntilIdle()
            assertEquals("2 operaciones sincronizadas", vm.uiState.value.mensaje)
            repo.pendientes.value = 1
            repo.syncResult = Result.failure(AppException(ErrorCode.RED, true, "Sin conexión"))
            vm.sincronizar()
            advanceUntilIdle()
            assertEquals(1, vm.uiState.value.pendientes)
            assertNull(vm.uiState.value.mensaje)
            assertTrue(vm.uiState.value.error.orEmpty().contains("Sin conexión"))
        } finally { store.clear() }
    }

    @Test
    fun gestionImpideDosEnviosYComunicaElResultadoConfirmado() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        val vm = GestionViewModel(repo)
        val store = ViewModelStore().apply { put("gestion", vm) }
        try {
            var confirmations = 0
            val venue = Recinto(nombre = "Sede", direccion = "Calle Uno")
            vm.guardarRecinto(venue) { confirmations++ }
            vm.guardarRecinto(venue) { confirmations++ }
            runCurrent()
            assertTrue(vm.estado.value.guardando)
            assertEquals(1, repo.venueWrites)
            assertEquals(0, confirmations)
            repo.venueResult.complete(Result.success(Unit))
            advanceUntilIdle()
            assertEquals(1, confirmations)
            assertFalse(vm.estado.value.guardando)
            assertFalse(vm.estado.value.error)
        } finally { store.clear() }
    }

    @Test
    fun gestionConErrorConservaElFormularioSinEjecutarLaConfirmacion() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        val vm = GestionViewModel(repo)
        val store = ViewModelStore().apply { put("gestion", vm) }
        try {
            var confirmed = false
            vm.guardarRecinto(Recinto(nombre = "Sede", direccion = "Calle Uno")) { confirmed = true }
            repo.venueResult.complete(Result.failure(AppException(ErrorCode.VALIDACION, false, "Revisa el recinto")))
            advanceUntilIdle()
            assertFalse(confirmed)
            assertTrue(vm.estado.value.error)
            assertFalse(vm.estado.value.guardando)
            assertEquals("Revisa el recinto", vm.estado.value.mensaje)
        } finally { store.clear() }
    }

    @Test
    fun detalleUsaElIdentificadorDeLaRutaYExponeElErrorDeConsulta() = runTest(main.dispatcher) {
        val repo = FakeRepository()
        val vm = EventoDetalleViewModel(SavedStateHandle(mapOf("id" to 42L)), repo)
        val store = ViewModelStore().apply { put("detalle", vm) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
            advanceUntilIdle()
            assertEquals(42L, repo.consultedEvent)
            assertNotNull(vm.uiState.value.error)
            assertFalse(vm.uiState.value.cargando)
        } finally { store.clear() }
    }
}
