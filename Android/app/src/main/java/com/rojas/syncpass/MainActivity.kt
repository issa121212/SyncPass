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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cognitoauth.CognitoAuthViewModel
import com.rojas.syncpass.core.config.CognitoAppConfig
import com.rojas.syncpass.ui.login.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.rojas.syncpass.ui.login.LoginScreen
import com.rojas.syncpass.ui.navigation.SyncPassApp
import com.rojas.syncpass.ui.theme.SyncPassTheme

/**
 * Punto de entrada. Cognito Hosted UI entrega los tokens; la sesión se guarda
 * en SessionStore y las pantallas de dominio usan el access token vía OkHttp.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val cognitoVm: CognitoAuthViewModel by viewModels()
    private val sessionVm: SessionViewModel by viewModels()

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

        setContent {
            SyncPassTheme {
                if (!CognitoAppConfig.isConfigured) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Falta configuración de Cognito (.env)")
                    }
                    return@SyncPassTheme
                }

                val sessionOwner by sessionVm.owner.collectAsState()
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
                        sessionVm.open(it)
                    }
                }

                val owner = sessionOwner
                if (owner == null) {
                    LoginScreen(
                        isLoading = cognitoState.isLoading,
                        error = cognitoState.error,
                        onCognitoClick = { cognitoVm.startLogin() },
                        onErrorShown = { cognitoVm.onErrorShown() },
                    )
                } else {
                    key(owner) {
                        CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                            SyncPassApp(
                                onLogout = {
                                    sessionVm.close()
                                    cognitoVm.startLogout()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
