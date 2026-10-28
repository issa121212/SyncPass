package com.cognitoauth

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import kotlin.coroutines.resume

/**
 * Fachada reutilizable sobre AppAuth-Android para Cognito (Authorization Code + PKCE).
 *
 * Responsabilidades:
 * - Descubrir endpoints vía `.well-known/openid-configuration` del issuer.
 * - Armar [AuthorizationRequest] con PKCE automático.
 * - Generar Intents de login/logout para lanzar con ActivityResultLauncher.
 * - Intercambiar el código por tokens.
 *
 * Lo que NO hace (lo hace la app): UI, navegación, persistencia de tokens,
 * ni registrar el callback en la consola de Cognito (ver docs/03_cognito.md).
 *
 * Uso mínimo:
 * ```
 * val manager = CognitoAuthManager(context, config)
 * val intent = manager.buildAuthIntent() // suspend: hace discovery
 * launcher.launch(intent)
 * // en onActivityResult:
 * manager.exchangeCode(data, onResult = { ... })
 * ```
 */
class CognitoAuthManager(
    context: Context,
    val config: CognitoConfig,
    appAuthConfig: AppAuthConfiguration = AppAuthConfiguration.Builder().build(),
) {
    init { config.validate() }

    private val authService = AuthorizationService(context.applicationContext, appAuthConfig)

    @Volatile
    private var serviceConfig: AuthorizationServiceConfiguration? = null

    /** Descubre (y cachea) la configuración OpenID del issuer. Suspende hasta responder. */
    suspend fun serviceConfiguration(): AuthorizationServiceConfiguration =
        serviceConfig ?: fetchServiceConfig().also { serviceConfig = it }

    private suspend fun fetchServiceConfig(): AuthorizationServiceConfiguration =
        suspendCancellableCoroutine { cont ->
            AuthorizationServiceConfiguration.fetchFromIssuer(
                config.issuerNetUri,
                AuthorizationServiceConfiguration.RetrieveConfigurationCallback { cfg, ex ->
                    if (cfg != null) cont.resume(cfg)
                    else cont.resumeWith(
                        Result.failure(ex ?: IllegalStateException("No se pudo descubrir Cognito"))
                    )
                },
            )
        }

    /** Arma el AuthorizationRequest con response_type=code y PKCE (AppAuth lo genera solo). */
    suspend fun buildAuthRequest(): AuthorizationRequest {
        val cfg = serviceConfiguration()
        return AuthorizationRequest.Builder(
            cfg,
            config.clientId,
            ResponseTypeValues.CODE,
            config.redirectNetUri,
        ).setScope(config.scopes).setPrompt("login").build()
    }

    /** Intent para abrir el Hosted UI (ej: `launcher.launch(manager.buildAuthIntent())`). */
    suspend fun buildAuthIntent(): Intent {
        val request = buildAuthRequest()
        return authService.getAuthorizationRequestIntent(request)
    }

    /**
     * Intercambia el código de autorización por tokens.
     * Llamar con el `Intent data` que llega al ActivityResultLauncher.
     * @param data Intent de respuesta (puede ser null si el usuario canceló).
     */
    fun exchangeCode(data: Intent?, onResult: (CognitoAuthResult) -> Unit) {
        val resp = data?.let { AuthorizationResponse.fromIntent(it) }
        val ex = data?.let { AuthorizationException.fromIntent(it) }

        if (resp == null) {
            if (ex != null) onResult(CognitoAuthResult.Failure(ex.message ?: "Autorización fallida", ex))
            else onResult(CognitoAuthResult.Cancelled)
            return
        }
        authService.performTokenRequest(
            resp.createTokenExchangeRequest(),
            AuthorizationService.TokenResponseCallback { token: TokenResponse?, tokenEx: AuthorizationException? ->
                if (token != null) {
                    onResult(
                        CognitoAuthResult.Success(
                            CognitoTokens(
                                idToken = token.idToken,
                                accessToken = token.accessToken,
                                refreshToken = token.refreshToken,
                                expiresIn = token.accessTokenExpirationTime?.let {
                                    (it - System.currentTimeMillis()) / 1000
                                },
                            )
                        )
                    )
                } else {
                    onResult(
                        CognitoAuthResult.Failure(
                            tokenEx?.message ?: "Intercambio de código fallido",
                            tokenEx,
                        )
                    )
                }
            },
        )
    }

    /**
     * Intent para cerrar sesión en el Hosted UI (End Session).
     * Requiere el idToken del login; si es null usa solo client_id+logout_uri.
     */
    suspend fun buildLogoutIntent(idToken: String? = null): Intent {
        val cfg = serviceConfiguration()
        val endpoint = cfg.authorizationEndpoint.buildUpon()
            .path("/logout").clearQuery()
            .appendQueryParameter("client_id", config.clientId)
            .appendQueryParameter("logout_uri", config.logoutUri)
            .build()
        return Intent(Intent.ACTION_VIEW, endpoint)
    }

    fun dispose() = authService.dispose()
}
