package com.rojas.syncpass

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.rojas.syncpass.ui.home.EventoDialog
import com.rojas.syncpass.ui.event.*
import com.rojas.syncpass.ui.components.FechaCampo
import com.rojas.syncpass.ui.theme.SyncPassTheme
import com.rojas.syncpass.domain.model.*
import org.junit.Rule
import org.junit.Test

class V14UiTest {
    @get:Rule val compose = createComposeRule()
    fun project(id: Long, name: String) = Proyecto(id, name, null, null, null, null, null, "PLANIFICADO", "Ana", null)

    @Test fun bothProjectsAreVisibleAndSelectableWithoutOpeningMenu() {
        compose.setContent { SyncPassTheme { EventoDialog(
            listOf(project(1, "Proyecto A"), project(2, "Proyecto B")), {},
            { _, _, _, _, _, _, _, _, _, _ -> }) } }
        compose.onNodeWithText("Proyecto A").assertIsDisplayed()
        compose.onNodeWithText("Proyecto B").assertIsDisplayed().performClick()
        compose.onNodeWithText("Proyecto B").assertIsSelected()
    }

    @Test fun datesAndHoursShowSeparatorsAndRemainEditable() {
        compose.setContent {
            var date by remember { mutableStateOf("") }; var time by remember { mutableStateOf("") }
            SyncPassTheme { Column {
                FechaCampo(date, { date = it }, "Fecha", Modifier.testTag("fecha"))
                FechaCampo(time, { time = it }, "Hora", Modifier.testTag("hora"), hora = true)
            } }
        }
        compose.onNodeWithTag("fecha").performTextInput("20261204")
        compose.onNodeWithTag("fecha").assertTextContains("2026-12-04")
        compose.onNodeWithTag("hora").performTextInput("1400")
        compose.onNodeWithTag("hora").assertTextContains("14:00")
        compose.onNodeWithTag("fecha").performTextClearance()
        compose.onNodeWithTag("fecha").performTextInput("20270228")
        compose.onNodeWithTag("fecha").assertTextContains("2027-02-28")
    }

    @Test fun participantButtonShowsQrWithoutConfirmingAttendance() {
        val event = Evento(7, 1, "Proyecto", "Evento", null, "2026-12-04T14:00:00",
            20, 1, 0, null, null, null, null, 200, null, "PROGRAMADO", true, false, false, null)
        val profile = Perfil(usuarioId = 3, nombre = "Ana", email = "ana@example.com",
            roles = "PARTICIPANTE", qrContenido = "ESTUDIANTE_3")
        compose.setContent { SyncPassTheme { EventoDetalleScreen(
            state = EventoDetalleUiState(evento = event, perfil = profile),
            onBack = {}, onRefresh = {}, onInscribirse = {}, onGuardarEncuesta = { _, _, _ -> },
            onEscanear = {}, onLimpiarMensaje = {}) } }
        compose.onNodeWithText("Mostrar QR para asistencia").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Código QR").assertIsDisplayed()
        compose.onNodeWithText("SYNC_PASS:ASISTENCIA:7:3").assertDoesNotExist()
        compose.onNodeWithText("Cerrar").performClick()
        compose.onNodeWithText("Estado: Inscrito, sin marcación").performScrollTo().assertIsDisplayed()
    }
}
