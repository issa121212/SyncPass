package com.cognitoauth

/**
 * Tokens del User Pool tras el intercambio código -> tokens.
 * @param idToken JWT para EndSession (logout) y para identificar al usuario.
 * @param accessToken Bearer para llamar a tu backend.
 * @param refreshToken Opcional, para renovar sin re-login.
 * @param expiresIn Segundos de vigencia (viene de TokenResponse).
 */
data class CognitoTokens(
    val idToken: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val expiresIn: Long? = null,
)

/** Resultado sellado del flujo para que la UI haga `when` exhaustivo. */
sealed interface CognitoAuthResult {
    data class Success(val tokens: CognitoTokens) : CognitoAuthResult
    data class Failure(val message: String, val cause: Throwable? = null) : CognitoAuthResult
    data object Cancelled : CognitoAuthResult
}
