package com.rojas.syncpass.core.jwt

import com.google.gson.annotations.SerializedName

data class SessionUser(
    val username: String = "",
    val name: String? = null,
    val roles: List<String> = emptyList(),
    // Cognito entrega los grupos en este claim (no en "roles").
    @SerializedName("cognito:groups")
    val cognitoGroups: List<String> = emptyList(),
    val iss: String = "",
    val sub: String = "",
    val iat: Long = 0,
    val exp: Long = 0
) {
    // Derivado: segundos restantes. Negativo = expirado.
    val secondsRemaining: Long get() = exp - (System.currentTimeMillis() / 1000)
    val isExpired: Boolean get() = secondsRemaining <= 0
}