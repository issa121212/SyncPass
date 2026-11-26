package com.rojas.syncpass

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.rojas.syncpass.data.mapper.toDomain
import com.rojas.syncpass.data.mapper.toEntity
import com.rojas.syncpass.data.remote.EventoResponse
import com.rojas.syncpass.domain.SyncPassOperations
import com.rojas.syncpass.domain.OperacionRechazada
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.ui.home.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class FiltroEstadoEventoTest {
    @get:Rule val main = MainDispatcherRule()
    private class Repository : SyncPassOperations by mock(SyncPassOperations::class.java) {
        override val perfil = MutableStateFlow<Perfil?>(null)
        override val proyectos = MutableStateFlow<List<Proyecto>>(emptyList())
        override val eventos = MutableStateFlow<List<Evento>>(emptyList())
        override val pendientes = MutableStateFlow(0)
        override val rechazadas = MutableStateFlow<List<OperacionRechazada>>(emptyList())
        override val recintos = MutableStateFlow<List<Recinto>>(emptyList())
        var requests = 0
        override suspend fun refrescar(): Result<Unit> { requests++; return Result.success(Unit) }
    }
    private fun event(id: Long, state: String) = EventoResponse(id = id, estado = state).toEntity().toDomain()

    @Test fun filtraCatalogoLocalSinPedirLaApiYActualizaResultadosSinPerderPendientes() = runTest(main.dispatcher) {
        val repo = Repository()
        val original = listOf(event(1, "PROGRAMADO"), event(2, "CANCELADO"),
            event(-3, "PROGRAMADO").copy(syncState = "PENDIENTE"), event(4, "EN_CURSO"), event(5, "FINALIZADO"))
        repo.eventos.value = original
        val vm = HomeViewModel(repo)
        val owner = ViewModelStore().apply { put("home", vm) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
            advanceUntilIdle()
            for (filter in FiltroEstadoEvento.entries) {
                vm.filtrarEstado(filter); advanceUntilIdle()
                val expected = when (filter) {
                    FiltroEstadoEvento.TODOS -> listOf(1L, 2L, -3L, 4L, 5L)
                    FiltroEstadoEvento.PROGRAMADO -> listOf(1L, -3L)
                    FiltroEstadoEvento.CANCELADO -> listOf(2L)
                    FiltroEstadoEvento.EN_CURSO -> listOf(4L)
                    FiltroEstadoEvento.FINALIZADO -> listOf(5L)
                }
                assertEquals(expected, vm.uiState.value.eventosVisibles.map { it.id })
            }
            vm.filtrarEstado(FiltroEstadoEvento.PROGRAMADO)
            repo.eventos.value = original.map { if (it.id == 1L) it.copy(estado = "CANCELADO") else it }
            advanceUntilIdle()
            assertEquals(listOf(-3L), vm.uiState.value.eventosVisibles.map { it.id })
            assertEquals("PENDIENTE", vm.uiState.value.eventosVisibles.single().syncState)
            assertEquals(1, repo.requests)
            vm.filtrarEstado(FiltroEstadoEvento.TODOS); advanceUntilIdle()
            assertEquals(repo.eventos.value, vm.uiState.value.eventosVisibles)
        } finally { owner.clear() }
    }

    @Test fun restauraSeleccionYPermiteVolverATodosCuandoElResultadoEstaVacio() = runTest(main.dispatcher) {
        val repo = Repository().apply { eventos.value = listOf(event(1, "PROGRAMADO")) }
        val saved = SavedStateHandle(mapOf("filtroEstadoEvento" to "CANCELADO"))
        val vm = HomeViewModel(repo, saved)
        val owner = ViewModelStore().apply { put("home", vm) }
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
            advanceUntilIdle()
            assertEquals(FiltroEstadoEvento.CANCELADO, vm.uiState.value.filtroEstado)
            assertTrue(vm.uiState.value.eventosVisibles.isEmpty())
            assertEquals(1, vm.uiState.value.eventos.size)
            vm.filtrarEstado(FiltroEstadoEvento.TODOS); advanceUntilIdle()
            assertEquals("TODOS", saved.get<String>("filtroEstadoEvento"))
            assertEquals(listOf(1L), vm.uiState.value.eventosVisibles.map { it.id })
        } finally { owner.clear() }
    }
}
