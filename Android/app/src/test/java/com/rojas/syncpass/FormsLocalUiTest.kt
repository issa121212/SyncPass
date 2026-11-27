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
class FormsLocalUiTest {
    @get:Rule val compose = createComposeRule()
    private val project = ProyectoResponse(id = 1, nombre = "Huerto", codigoPublico = "PRY-ABCDEF123456").toEntity().toDomain()
    private val event = EventoResponse(id = 7, proyectoId = 1, nombre = "Taller", fechaHora = "2026-12-04T14:00:45.123", cupos = 10, inscritos = 2).toEntity().toDomain()

    @Test fun editarMantieneSegundosYPermiteReutilizarRecintoConZona() {
        var saved: EventoInput? = null
        compose.setContent { SyncPassTheme { EventoEditorDialog(listOf(project), {}, { saved = it }, inicial = event,
            recintos = listOf(Recinto(9, "Sede", "Calle 100", -33.45, -70.66, 350))) } }
        compose.onNodeWithText("Nombre *").performTextReplacement("Taller actualizado")
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Seleccionar recinto").performClick()
        compose.onNodeWithText("Sede").performClick()
        compose.onNodeWithText("Zona seleccionada · Radio de 350 m").assertExists()
        compose.onNodeWithText("Guardar", useUnmergedTree = true).performClick()
        assertEquals("2026-12-04T14:00:45.123", saved?.fechaHora)
        assertEquals("Taller actualizado", saved?.nombre)
        assertEquals(-33.45, saved?.latitud)
        assertEquals(350, saved?.radioMetros)
    }

    @Test fun formularioValidaCoordenadasIncompletasAntesDeEnviar() {
        var saved = false
        compose.setContent { SyncPassTheme { EventoEditorDialog(listOf(project), {}, { saved = true }, inicial = event) } }
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Siguiente").performClick()
        compose.onNodeWithText("Coordenadas avanzadas").performScrollTo().performClick()
        compose.onNodeWithText("Latitud").performScrollTo().performTextInput("-33.45")
        compose.onNodeWithText("Guardar", useUnmergedTree = true).performClick()
        assertFalse(saved)
        compose.onNodeWithText("Completa ambas coordenadas").assertExists()
    }

    @Test fun proyectoNoCierraNiEnviaConFechasInvertidasYLuegoSePuedeCorregir() {
        var saved: List<String>? = null
        var dismissed = false
        compose.setContent { SyncPassTheme { com.rojas.syncpass.ui.home.ProyectoDialog({ dismissed = true },
            { a,b,c,d,e,f -> saved = listOf(a,b,c,d,e,f) }) } }
        compose.onNodeWithText("Guardar").assertIsNotEnabled()
        compose.onNodeWithText("Nombre *").performTextInput(" Huerto ")
        compose.onNodeWithText("Inicio (AAAA-MM-DD)").performScrollTo().performTextInput("20261204")
        compose.onNodeWithText("Fin (AAAA-MM-DD)").performScrollTo().performTextInput("20261201")
        compose.onNodeWithText("Guardar").performClick()
        assertNull(saved)
        assertFalse(dismissed)
        compose.onNodeWithText("Fin (AAAA-MM-DD)").performScrollTo().performTextReplacement("20261205")
        compose.onNodeWithText("Guardar").performClick()
        assertEquals("Huerto", saved?.get(0))
        assertEquals("2026-12-05", saved?.get(5))
        assertTrue(dismissed)
    }

    @Test fun loginEvitaDobleInicioMientrasCargaYMuestraError() {
        val loading = mutableStateOf(false)
        var starts = 0
        compose.setContent { SyncPassTheme { com.rojas.syncpass.ui.login.LoginScreen(loading.value, "Servicio no disponible", { starts++ }) } }
        compose.onNodeWithText("Servicio no disponible").assertIsDisplayed()
        compose.onNodeWithText("Continuar con Cognito").performClick()
        assertEquals(1, starts)
        compose.runOnIdle { loading.value = true }
        compose.onAllNodes(hasClickAction()).filterToOne(hasText("Continuar con Cognito")).assertDoesNotExist()
        compose.onAllNodes(hasClickAction()).onFirst().assertIsNotEnabled()
    }
}
