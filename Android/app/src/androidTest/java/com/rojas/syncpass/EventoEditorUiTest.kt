package com.rojas.syncpass

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.ui.event.EventoEditorDialog
import com.rojas.syncpass.ui.theme.SyncPassTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EventoEditorUiTest {
    @get:Rule val compose = createComposeRule()
    private val proyecto = Proyecto(1, "Huerto", null, null, null, null, null, "PLANIFICADO", "Ana", null, 8)
    private val evento = Evento(7, 1, "Huerto", "Taller", "Descripción", "2026-12-04T14:00:45.123",
        5, 2, 0, null, null, null, null, 200, "EVENTO_7", "PROGRAMADO", false, false, false, null)

    @Test fun editUsesThreeStepsAndPreservesExistingSeconds() {
        var saved: EventoInput? = null
        compose.setContent { SyncPassTheme { EventoEditorDialog(listOf(proyecto), {}, { saved = it }, inicial = evento) } }
        compose.onNodeWithText("Editar evento · 1/3").assertIsDisplayed()
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Editar evento · 2/3").assertIsDisplayed()
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Guardar", useUnmergedTree = true).performClick()
        compose.runOnIdle {
            assertEquals("2026-12-04T14:00:45.123", saved?.fechaHora)
            assertEquals(2, evento.inscritos)
        }
    }

    @Test fun cannotReduceCapacityBelowExistingRegistrations() {
        compose.setContent { SyncPassTheme { EventoEditorDialog(listOf(proyecto), {}, {}, inicial = evento) } }
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Cupos (0 = sin límite)").performTextClearance()
        compose.onNodeWithText("Cupos (0 = sin límite)").performTextInput("1")
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("El cupo no puede ser menor que los inscritos").performScrollTo().assertIsDisplayed()
    }

    @Test fun serverErrorDoesNotCloseEditorAndBusyDisablesSubmission() {
        compose.setContent { SyncPassTheme { EventoEditorDialog(listOf(proyecto), {}, {}, inicial = evento,
            guardando = true, errorGuardado = "No se pudo conectar con el servidor") } }
        compose.onNodeWithText("Guardando…").assertIsNotEnabled()
        compose.onNodeWithText("No se pudo conectar con el servidor").performScrollTo().assertIsDisplayed()
    }
}
