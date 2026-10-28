package com.cognitoauth

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado observable del login Cognito para Compose o Views. */
data class CognitoUiState(
    val isLoading: Boolean = false,
    val tokens: CognitoTokens? = null,
    val error: String? = null,
    /** Intent listo para lanzar con ActivityResultLauncher (se consume una vez). */
    val pendingAuthIntent: Intent? = null,
    val pendingLogoutIntent: Intent? = null,
)

/**
 * ViewModel reutilizable: la UI solo observa [uiState] y lanza intents.
 *
 * Flujo:
 * 1. `startLogin()` -> emite `pendingAuthIntent` (la Activity lo lanza).
 * 2. Activity recibe `data` -> `onAuthResponse(data)` -> intercambia código -> `tokens`.
 * 3. `startLogout(idToken)` -> emite `pendingLogoutIntent`.
 * 4. `consumeAuthIntent()/consumeLogoutIntent()` limpian el intent tras lanzarlo.
 */
class CognitoAuthViewModel(
    private val manager: CognitoAuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CognitoUiState())
    val uiState: StateFlow<CognitoUiState> = _uiState.asStateFlow()

    val isLoggedIn: Boolean get() = _uiState.value.tokens?.accessToken != null

    /** Reporta un error externo (ej: el launcher no pudo abrir el navegador). */
    fun reportError(message: String) {
        _uiState.update { it.copy(isLoading = false, error = message) }
    }

    fun startLogin() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { manager.buildAuthIntent() }
                .onSuccess { intent ->
                    _uiState.update { it.copy(isLoading = false, pendingAuthIntent = intent) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            // Mensaje con causa para diagnóstico (MVP): ej "Unable to resolve host",
                            // "redirect_uri mismatch", etc. La UI lo muestra en Snackbar.
                            error = "No se pudo iniciar Cognito. Revisa la conexion y configuracion.",
                        )
                    }
                }
        }
    }

    fun onAuthResponse(data: Intent?) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        manager.exchangeCode(data) { result ->
            when (result) {
                is CognitoAuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, tokens = result.tokens) }
                }
                is CognitoAuthResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                CognitoAuthResult.Cancelled ->
                    _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun startLogout() {
        val idToken = _uiState.value.tokens?.idToken
        _uiState.update { it.copy(isLoading = true, error = null, tokens = null) }
        viewModelScope.launch {
            runCatching { manager.buildLogoutIntent(idToken) }
                .onSuccess { intent ->
                    _uiState.update {
                        it.copy(isLoading = false, tokens = null, pendingLogoutIntent = intent)
                    }
                }
                .onFailure {
                    // Sin intent igual limpiamos sesión local.
                    _uiState.update { it.copy(isLoading = false, tokens = null) }
                }
        }
    }

    fun onLogoutCompleted() {
        _uiState.update { it.copy(tokens = null, isLoading = false) }
    }

    fun consumeAuthIntent(): Intent? {
        val intent = _uiState.value.pendingAuthIntent ?: return null
        _uiState.update { it.copy(pendingAuthIntent = null) }
        return intent
    }

    fun consumeLogoutIntent(): Intent? {
        val intent = _uiState.value.pendingLogoutIntent ?: return null
        _uiState.update { it.copy(pendingLogoutIntent = null) }
        return intent
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        manager.dispose()
        super.onCleared()
    }

    companion object {
        const val TAG = "CognitoAuth"
    }
}
