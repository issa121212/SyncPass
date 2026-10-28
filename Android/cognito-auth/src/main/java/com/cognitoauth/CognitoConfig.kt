package com.cognitoauth

import android.net.Uri

/**
 * Configuración del User Pool de Cognito.
 *
 * Ningún valor va en duro en el módulo: la app anfitriona construye
 * este objeto (idealmente desde BuildConfig/.env) y se lo pasa al manager.
 *
 * Valores de ejemplo (ver cognito-auth/README.md Paso 3):
 * - issuerUri = issuer de tu User Pool (https://cognito-idp.<region>.amazonaws.com/<poolId>)
 * - clientId = app client ID de tu User Pool
 * - redirectUri = "com.tuempresa.tuapp://cognito/callback" (custom scheme)
 *   o tu URL HTTPS de CloudFront (requiere App Links)
 * - scopes = "email openid phone"
 */
data class CognitoConfig(
    val issuerUri: String,
    val clientId: String,
    val redirectUri: String,
    val logoutUri: String = redirectUri,
    val scopes: String = "email openid phone",
) {
    val issuerNetUri: Uri get() = Uri.parse(issuerUri)
    val redirectNetUri: Uri get() = Uri.parse(redirectUri)

    fun validate() {
        require(issuerUri.isNotBlank()) { "CognitoConfig.issuerUri vacío" }
        require(clientId.isNotBlank()) { "CognitoConfig.clientId vacío" }
        require(redirectUri.isNotBlank()) { "CognitoConfig.redirectUri vacío" }
    }
}
