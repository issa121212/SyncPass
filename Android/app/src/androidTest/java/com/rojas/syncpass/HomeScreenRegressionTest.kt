package com.rojas.syncpass

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollTo
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.ui.home.*
import com.rojas.syncpass.ui.theme.SyncPassTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun eventAndProjectWithSameIdCanBeDisplayedTogether() {
        val project = Proyecto(1, "Proyecto prueba", null, null, null,
            null, null, "PLANIFICADO", null, null)
        val event = Evento(1, 1, "Proyecto prueba", "Evento prueba", null,
            "2026-10-26T12:00:00Z", 10, 0, 0, null, null, null, null,
            200, null, "PROGRAMADO", false, false, false, null)
        val profile = Perfil(usuarioId = 1, nombre = "Prueba", email = "test@example.com",
            roles = "ESTUDIANTE", qrContenido = "ESTUDIANTE_1")
        compose.setContent {
            SyncPassTheme {
                HomeScreen(
                    state = HomeUiState(perfil = profile, proyectos = listOf(project), eventos = listOf(event)),
                    onRefresh = {}, onSync = {}, onEventClick = {}, onLogout = {},
                    onCrearProyecto = { _, _, _, _, _, _ -> },
                    onCrearEvento = { _, _, _, _, _, _, _, _, _, _ -> },
                    onLimpiarMensaje = {})
            }
        }
        compose.onNodeWithText("Evento prueba").assertIsDisplayed()
        compose.onNodeWithText("Proyectos comunitarios").performScrollTo().assertIsDisplayed()
    }
}
