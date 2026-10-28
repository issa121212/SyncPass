package com.rojas.syncpass.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rojas.syncpass.ui.components.AuthCard
import com.rojas.syncpass.ui.theme.SyncPassTheme

// LoginScreen = entrada Cognito-only (módulo :cognito-auth, ver cognito-auth/README.md).
// Sin formulario clásico: un botón abre el Hosted UI en el navegador y el
// intercambio código -> tokens lo hace CognitoAuthViewModel en MainActivity.
// Esta Screen solo dibuja estado (loading/error) y reenvía eventos: UI tonta.
@Composable
fun LoginScreen(
    isLoading: Boolean = false,
    error: String? = null,
    onCognitoClick: () -> Unit = {},
    onErrorShown: () -> Unit = {},
) {
    LoginContent(
        isLoading = isLoading,
        error = error,
        onCognitoClick = onCognitoClick,
        onErrorShown = onErrorShown,
    )
}

@Composable
fun LoginContent(
    isLoading: Boolean,
    error: String?,
    onCognitoClick: () -> Unit,
    onErrorShown: () -> Unit,
) {
    // SnackbarHostState en remember = sobrevive a recomposiciones (el error vive en el ViewModel).
    val snackbar = remember { SnackbarHostState() }

    // LaunchedEffect(error) = muestra el Snackbar una vez por cada error nuevo
    // y avisa onErrorShown() para limpiarlo y no repetirlo en cada recomposición.
    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            onErrorShown()
        }
    }

    // Column raíz = centra el Card en pantalla con margen exterior de 16dp.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthCard(
            title = "SyncPass",
            subtitle = "Inicia sesión para acreditar asistencia"
        ) {
            Text(
                "Serás redirigido al inicio de sesión seguro de tu organización.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Button Ingresar con Cognito: height(48.dp) = mínimo táctil.
            // Si isLoading muestra CircularProgressIndicator (feedback inmediato).
            Button(
                onClick = onCognitoClick,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Continuar con Cognito")
                }
            }
        }
        // SnackbarHost = punto donde LaunchedEffect muestra el snackbar de error.
        SnackbarHost(hostState = snackbar)
    }
}

// Preview con estado ficticio: ningún ViewModel involucrado.
@Preview(showBackground = true)
@Composable
private fun LoginContentPreview() {
    SyncPassTheme {
        LoginContent(
            isLoading = false,
            error = null,
            onCognitoClick = {},
            onErrorShown = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentLoadingPreview() {
    SyncPassTheme {
        LoginContent(
            isLoading = true,
            error = null,
            onCognitoClick = {},
            onErrorShown = {}
        )
    }
}
