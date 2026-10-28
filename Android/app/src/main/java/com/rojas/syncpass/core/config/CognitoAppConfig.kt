package com.rojas.syncpass.core.config

import com.cognitoauth.CognitoConfig
import com.rojas.syncpass.BuildConfig

/**
 * Único punto de acceso a la config Cognito (ver cognito-auth/README.md Paso 3).
 * Todo viene de BuildConfig (.env); ningún .kt hardcodea issuer/clientId.
 */
object CognitoAppConfig {
    val config = CognitoConfig(
        issuerUri = BuildConfig.COGNITO_ISSUER,
        clientId = BuildConfig.COGNITO_CLIENT_ID,
        redirectUri = BuildConfig.COGNITO_REDIRECT_URI,
        logoutUri = BuildConfig.COGNITO_LOGOUT_URI,
        scopes = BuildConfig.COGNITO_SCOPES,
    )

    /** false si falta .env (compilación CI sin secretos): se muestra error en vez de crashear. */
    val isConfigured: Boolean =
        BuildConfig.COGNITO_ISSUER.isNotBlank() && BuildConfig.COGNITO_CLIENT_ID.isNotBlank()
}
