package com.rojas.syncpass

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.mutableStateOf
import com.rojas.syncpass.data.mapper.*
import com.rojas.syncpass.data.remote.*
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.ui.event.*
import com.rojas.syncpass.ui.theme.SyncPassTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EventDetailLocalUiTest {
    @get:Rule val compose = createComposeRule()
    private val event = EventoResponse(id = 7, proyectoId = 1, nombre = "Taller", proyectoNombre = "Huerto", fechaHora = "2026-12-04T14:00:00", cupos = 10, descripcion = "Actividad vecinal", recinto = "Sede", direccion = "Calle 100").toEntity().toDomain()
    private val participant = Perfil(usuarioId = 3, nombre = "Ana", email = "ana@example.invalid", roles = "PARTICIPANTE", qrContenido = "")
    private fun show(state: EventoDetalleUiState, register: () -> Unit = {}, survey: (Int, Int, String) -> Unit = { _, _, _ -> }, cancel: () -> Unit = {}) {
        compose.setContent { SyncPassTheme { EventoDetalleScreen(state, {}, {}, register, survey, {}, {}, {}, {}, cancel, {}) } }
    }

    @Test fun participanteSeInscribeYLuegoPresentaQrSinAutomarcarse() {
        var registrations = 0
        val state = mutableStateOf(EventoDetalleUiState(evento = event, perfil = participant))
        compose.setContent { SyncPassTheme { EventoDetalleScreen(state.value, {}, {}, { registrations++ }, { _, _, _ -> }, {}, {}, {}, {}, {}, {}) } }
        compose.onNodeWithText("Gestionar").assertDoesNotExist()
        compose.onNodeWithText("Inscribirme en el evento").performScrollTo().performClick()
        assertEquals(1, registrations)
        compose.runOnIdle { state.value = state.value.copy(evento = event.copy(inscrito = true)) }
        compose.onNodeWithText("Mostrar QR para asistencia").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Código QR").assertIsDisplayed()
        compose.onNodeWithText("Cerrar").performClick()
        compose.onNodeWithText("Estado: Inscrito, sin marcación").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Escanear QR de participante").assertDoesNotExist()
    }

    @Test fun encuestaSoloDespuesDeAsistirYEnviaValoresElegidos() {
        var answer: Triple<Int, Int, String>? = null
        show(EventoDetalleUiState(evento = event.copy(inscrito = true, esPresente = true), perfil = participant), survey = { a, b, c -> answer = Triple(a,b,c) })
        compose.onNodeWithText("Responder encuesta de satisfacción").performScrollTo().performClick()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.SetProgress))[0].performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(5f) }
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.SetProgress))[1].performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(3f) }
        compose.onNodeWithText("Comentarios (opcional)").performTextInput("Muy útil")
        compose.onNodeWithText("Enviar").performClick()
        assertEquals(Triple(5,3,"Muy útil"), answer)
        compose.onNodeWithText("Encuesta de satisfacción").assertDoesNotExist()
    }

    @Test fun docenteConHistorialPuedeCancelarPeroNoEliminar() {
        val project = ProyectoResponse(id = 1, nombre = "Huerto", responsable = ResponsableResponse(id = 3)).toEntity().toDomain()
        var cancelled = 0
        show(EventoDetalleUiState(evento = event.copy(inscritos = 2), perfil = participant.copy(roles = "DOCENTE"), proyectos = listOf(project),
            asistentes = listOf(Asistente(usuarioId = 4, nombre = "Luis", email = "luis@example.invalid", presente = true, marcadoEn = "2026-12-04T14:00:00Z", encuesta = Encuesta(satisfaccion = 5, utilidad = 4))),
            encuestas = listOf(EncuestaEvento(estudiante = Persona(nombre = "Luis"), satisfaccion = 5, utilidad = 4, comentarios = "Excelente"))), cancel = { cancelled++ })
        compose.onNodeWithText("Encuestas recibidas").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Excelente").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Gestionar").performClick()
        compose.onNodeWithText("Eliminar (tiene historial)").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar evento").performClick()
        compose.onNodeWithText("Se conservarán sus inscripciones y asistencias.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Confirmar").performClick()
        assertEquals(1, cancelled)
    }

    @Test fun estudiantePuedeEscanearPeroNoAdministrarNiVerEncuestas() {
        show(EventoDetalleUiState(evento = event, perfil = participant.copy(roles = "ESTUDIANTE")))
        compose.onNodeWithText("Escanear QR de participante").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Gestionar").assertDoesNotExist()
        compose.onNodeWithText("Encuestas recibidas").assertDoesNotExist()
        compose.onNodeWithText("Inscribirme en el evento").assertDoesNotExist()
    }
}
