package com.rojas.syncpass

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cognitoauth.CognitoAuthManager
import com.cognitoauth.CognitoAuthViewModel
import com.rojas.syncpass.core.config.CognitoAppConfig
import com.rojas.syncpass.ui.login.CognitoViewModelFactory
import com.rojas.syncpass.ui.login.LoginScreen
import com.rojas.syncpass.ui.navigation.SyncPassApp
import com.rojas.syncpass.ui.theme.SyncPassTheme

/**
 * Punto de entrada. Cognito Hosted UI entrega los tokens; la sesión se guarda
 * en SessionStore y las pantallas de dominio usan el access token vía OkHttp.
 */
class MainActivity : ComponentActivity() {

    private val cognitoManager: CognitoAuthManager? by lazy {
        if (CognitoAppConfig.isConfigured) CognitoAuthManager(this, CognitoAppConfig.config)
        else null
    }

    private val cognitoVm: CognitoAuthViewModel by viewModels {
        cognitoManager
            ?.let { CognitoViewModelFactory(it) }
            ?: throw IllegalStateException("Cognito no configurado (.env)")
    }

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res -> cognitoVm.onAuthResponse(res.data) }

    private val logoutLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { cognitoVm.onLogoutCompleted() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!CognitoAppConfig.isConfigured) {
            Toast.makeText(
                this,
                "Cognito no configurado: completa .env (ver cognito-auth/README.md)",
                Toast.LENGTH_LONG,
            ).show()
        }
        enableEdgeToEdge()

        val container = (application as SyncPassApplication).container
        val sessionStore = container.sessionStore

        setContent {
            SyncPassTheme {
                if (cognitoManager == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Falta configuración de Cognito (.env)")
                    }
                    return@SyncPassTheme
                }

                var loggedIn by remember { mutableStateOf(false) }
                val cognitoState by cognitoVm.uiState.collectAsState()

                LaunchedEffect(cognitoState.pendingAuthIntent) {
                    cognitoState.pendingAuthIntent?.let {
                        runCatching { authLauncher.launch(it) }
                            .onFailure { e ->
                                cognitoVm.reportError("Cognito: no se pudo abrir el navegador (${e.message})")
                            }
                        cognitoVm.consumeAuthIntent()
                    }
                }
                LaunchedEffect(cognitoState.pendingLogoutIntent) {
                    cognitoState.pendingLogoutIntent?.let {
                        runCatching { logoutLauncher.launch(it) }
                            .onFailure { e ->
                                cognitoVm.reportError("Cognito: no se pudo abrir el navegador (${e.message})")
                            }
                        cognitoVm.consumeLogoutIntent()
                    }
                }
                LaunchedEffect(cognitoState.tokens) {
                    cognitoState.tokens?.let {
                        sessionStore.save(it)
                        loggedIn = true
                    }
                }

                if (!loggedIn) {
                    LoginScreen(
                        isLoading = cognitoState.isLoading,
                        error = cognitoState.error,
                        onCognitoClick = { cognitoVm.startLogin() },
                        onErrorShown = { cognitoVm.onErrorShown() },
                    )
                } else {
                    SyncPassApp(
                        repository = container.repository,
                        onLogout = {
                            loggedIn = false
                            sessionStore.clear()
                            // Limpia la sesión de Cognito si el Hosted UI sigue disponible.
                            cognitoVm.startLogout()
                        },
                    )
                }
            }
        }
    }
}
