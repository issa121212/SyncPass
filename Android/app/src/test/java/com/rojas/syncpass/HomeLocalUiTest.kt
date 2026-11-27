package com.rojas.syncpass

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.rojas.syncpass.ui.home.*
import com.rojas.syncpass.ui.theme.SyncPassTheme
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.data.mapper.*
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
class HomeLocalUiTest {
    @get:Rule val compose = createComposeRule()
    private val profile = Perfil(usuarioId = 3, nombre = "Ana", email = "ana@example.invalid", roles = "PARTICIPANTE", qrContenido = "")
    private fun show(state: HomeUiState, logout: () -> Unit = {}) {
        compose.setContent { SyncPassTheme { HomeScreen(state, {}, {}, {}, logout,
            { _, _, _, _, _, _ -> }, { _, _, _, _, _, _, _, _, _, _ -> }, {}) } }
    }
    @Test fun participanteVeSuInicioSinAccionesDeAdministracionYPuedeSalir() {
        var exited = false
        show(HomeUiState(perfil = profile), { exited = true })
        compose.onNodeWithText("Nuevo proyecto").assertDoesNotExist()
        compose.onNodeWithText("Nuevo evento").assertDoesNotExist()
        compose.onNodeWithText("Salir").performClick()
        assertTrue(exited)
    }

    @Test fun perfilPendienteMuestraCargaYNoPermiteCrear() {
        show(HomeUiState())
        compose.onNodeWithText("Cargando perfil...").assertIsDisplayed()
        compose.onNodeWithText("Nuevo evento").assertDoesNotExist()
    }

    @Test fun docentePuedeAbrirProyectoPeroBloqueaCreacionMientrasGuarda() {
        val state = androidx.compose.runtime.mutableStateOf(HomeUiState(perfil = profile.copy(roles = "DOCENTE")))
        compose.setContent { SyncPassTheme { HomeScreen(state.value, {}, {}, {}, {},
            { _, _, _, _, _, _ -> }, { _, _, _, _, _, _, _, _, _, _ -> }, {}) } }
        compose.onNodeWithText("Nuevo proyecto").performClick()
        compose.onNodeWithText("Nuevo proyecto comunitario").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { state.value = state.value.copy(creando = true) }
        compose.onNodeWithText("Nuevo proyecto").assertIsNotEnabled()
        compose.onNodeWithText("Nuevo evento").assertIsNotEnabled()
    }

    @Test fun participanteVeQrEtiquetasYCopiaLocalSinMostrarIdentificadorInterno() {
        val event = com.rojas.syncpass.data.remote.EventoResponse(id = 998877, nombre = "Taller vecinal", proyectoNombre = "Huerto", fechaHora = "2026-12-04T14:00:00", inscrito = true, esPresente = true, encuestaRespondida = true, recinto = "Sede", codigoPublico = "EVT-ABCDEF123456").toEntity().toDomain()
        var selected = 0L
        compose.setContent { SyncPassTheme { HomeScreen(HomeUiState(perfil = profile.copy(qrContenido = "CREDENCIAL_DE_PRUEBA"), eventos = listOf(event)), {}, {}, { selected = it }, {},
            { _, _, _, _, _, _ -> }, { _, _, _, _, _, _, _, _, _, _ -> }, {}) } }
        compose.onNodeWithText("Taller vecinal").performScrollTo().performClick()
        assertEquals(998877L, selected)
        compose.onNodeWithText("Asistió").assertExists()
        compose.onNodeWithText("Encuesta lista").assertExists()
        compose.onNodeWithText("EVT-ABCDEF123456").assertExists()
        compose.onNodeWithText("998877", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Mi QR").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Código QR").assertIsDisplayed()
        compose.onNodeWithText("Cerrar").performClick()
        compose.onNodeWithText("Mi credencial SyncPass").assertDoesNotExist()
    }

    @Test fun filtroSinResultadosConservaProyectoYRechazosVisibles() {
        val project = com.rojas.syncpass.data.remote.ProyectoResponse(id = -1, nombre = "Proyecto pendiente", areaTematica = "Salud", unidadVecinal = "Barrio", responsable = com.rojas.syncpass.data.remote.ResponsableResponse(nombre = "Ana")).toEntity().toDomain().copy(syncState = "PENDIENTE")
        show(HomeUiState(perfil = profile.copy(roles = "ESTUDIANTE"), proyectos = listOf(project), pendientes = 2,
            rechazadas = listOf(com.rojas.syncpass.domain.OperacionRechazada("ASISTENCIA", 1, "Fuera del radio"))))
        compose.onNodeWithText("Operaciones rechazadas").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Fuera del radio", substring = true).assertExists()
        compose.onNodeWithText("Proyecto pendiente").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Responsable: Ana").assertExists()
    }
}
