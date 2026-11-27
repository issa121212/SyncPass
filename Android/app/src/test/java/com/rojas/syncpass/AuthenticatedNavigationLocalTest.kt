package com.rojas.syncpass

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.cognitoauth.CognitoTokens
import com.rojas.syncpass.data.mapper.*
import com.rojas.syncpass.data.remote.*
import com.rojas.syncpass.data.repository.SyncPassRepository
import com.rojas.syncpass.data.session.SessionStore
import com.rojas.syncpass.domain.OperacionRechazada
import com.rojas.syncpass.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.MockedConstruction
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Grafo Hilt, navegación y ViewModels reales; sesión y repositorio son entornos falsos. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = HiltLocalTestApplication::class, qualifiers = "h891dp")
class AuthenticatedNavigationLocalTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var sessions: MockedConstruction<SessionStore>
    private lateinit var repositories: MockedConstruction<SyncPassRepository>
    private val project = ProyectoResponse(id = 1,nombre = "Huerto",responsable = ResponsableResponse(id = 3)).toEntity().toDomain()
    private val event = EventoResponse(id = 7,proyectoId = 1,proyectoNombre = "Huerto",nombre = "Taller navegable",fechaHora = "2026-12-04T14:00:00").toEntity().toDomain()
    private val dependencies = object : ExternalResource() {
        override fun before() {
            sessions = mockConstruction(SessionStore::class.java) { session, _ ->
                `when`(session.hasValidSession()).thenReturn(true)
                `when`(session.accountKey()).thenReturn("simulated-issuer|docente")
                `when`(session.tokens).thenReturn(MutableStateFlow(CognitoTokens(idToken = null,accessToken = "simulated",refreshToken = null)))
            }
            repositories = mockConstruction(SyncPassRepository::class.java) { repo, _ ->
                `when`(repo.perfil).thenReturn(MutableStateFlow(Perfil(usuarioId = 3,nombre = "Ana",email = "ana@example.invalid",roles = "DOCENTE",qrContenido = "")))
                `when`(repo.proyectos).thenReturn(MutableStateFlow(listOf(project)))
                `when`(repo.eventos).thenReturn(MutableStateFlow(listOf(event)))
                `when`(repo.recintos).thenReturn(MutableStateFlow(emptyList<Recinto>()))
                `when`(repo.pendientes).thenReturn(MutableStateFlow(0))
                `when`(repo.rechazadas).thenReturn(MutableStateFlow(emptyList<OperacionRechazada>()))
                `when`(repo.observarEvento(7)).thenReturn(MutableStateFlow(event))
                runBlocking {
                    `when`(repo.refrescar()).thenReturn(Result.success(Unit))
                    `when`(repo.refrescarEvento(7)).thenReturn(Result.success(event))
                    `when`(repo.asistentes(7)).thenReturn(Result.success(emptyList()))
                    `when`(repo.encuestas(7)).thenReturn(Result.success(emptyList()))
                }
            }
        }
        override fun after() { repositories.close(); sessions.close() }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(dependencies).around(compose)

    @Test fun docenteNavegaDeInicioAGestionDetalleYRecepcionSinAccederARed() {
        compose.onNodeWithText("Hola, Ana").assertIsDisplayed()
        compose.onNodeWithText("Administrar proyectos y recintos").performScrollTo().performClick()
        compose.onNodeWithText("Administrar").assertIsDisplayed()
        compose.onNodeWithText("Huerto").assertIsDisplayed()
        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithText("Taller navegable").performScrollTo().performClick()
        compose.onNodeWithText("Gestionar").assertIsDisplayed()
        compose.onNodeWithText("Escanear QR de participante").performScrollTo().performClick()
        compose.onNodeWithText("Permitir cámara").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Gestionar").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Hola, Ana").performScrollTo().assertIsDisplayed()
    }
}
