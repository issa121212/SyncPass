package com.rojas.syncpass

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.rojas.syncpass.data.mapper.*
import com.rojas.syncpass.data.remote.EventoResponse
import com.rojas.syncpass.domain.model.Perfil
import com.rojas.syncpass.ui.home.*
import com.rojas.syncpass.ui.theme.SyncPassTheme
import org.junit.Rule
import org.junit.Test

class HomeFilterUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectorMuestraSoloEstadoElegidoYCodigoPublicoSinMostrarIdInterno() {
        val events = listOf(
            EventoResponse(id = 998877, nombre = "Programado visible", fechaHora = "2026-12-04T14:30:00",
                codigoPublico = "EVT-ABCDEF123456").toEntity().toDomain(),
            EventoResponse(id = 998878, nombre = "Cancelado visible", estado = "CANCELADO",
                fechaHora = "2026-12-04T14:30:00", codigoPublico = "EVT-ABCDEF654321").toEntity().toDomain())
        val state = mutableStateOf(HomeUiState(
            perfil = Perfil(usuarioId = 3, nombre = "Docente de prueba", email = "test@example.invalid", roles = "DOCENTE", qrContenido = ""),
            eventos = events))
        compose.setContent { SyncPassTheme {
            HomeScreen(state = state.value, onRefresh = {}, onSync = {}, onEventClick = {}, onLogout = {},
                onCrearProyecto = { _, _, _, _, _, _ -> }, onCrearEvento = { _, _, _, _, _, _, _, _, _, _ -> },
                onLimpiarMensaje = {}, onFiltrarEstado = { filter ->
                    state.value = state.value.copy(filtroEstado = filter, eventosVisibles = filter.aplicar(events))
                })
        } }
        compose.onNodeWithText("Estado: Todos ▾").performScrollTo().performClick()
        compose.onNodeWithText("Cancelado", useUnmergedTree = true).performClick()
        compose.onNodeWithText("1 evento").assertExists()
        compose.onNodeWithText("Cancelado visible").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("EVT-ABCDEF654321").assertIsDisplayed()
        compose.onNodeWithText("Programado visible").assertDoesNotExist()
        compose.onNodeWithText("998878", substring = true).assertDoesNotExist()
    }
}
