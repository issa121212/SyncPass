package com.rojas.syncpass.data.session

import android.content.Context
import com.cognitoauth.CognitoTokens
import com.rojas.syncpass.core.jwt.JwtDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistencia local de la sesión Cognito.
 *
 * Tokens cifrados mediante una clave AES de Android Keystore.
 * La sesión expirada requiere iniciar sesión nuevamente; no se conserva refresh token.
 */
class SessionStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val vault = TokenVault()

    private val _tokens = MutableStateFlow(readTokens())
    val tokens: StateFlow<CognitoTokens?> = _tokens.asStateFlow()

    fun accountKey(): String? = currentAccessToken()?.let { token ->
        JwtDecoder.decode(token).getOrNull()?.let {
            if (it.sub.isBlank() || it.iss.isBlank()) null else "${it.iss}|${it.sub}"
        }
    }

    fun currentAccessToken(): String? = _tokens.value?.accessToken

    fun currentIdToken(): String? = _tokens.value?.idToken

    fun cognitoName(): String? {
        val access = currentAccessToken()?.let { JwtDecoder.decode(it).getOrNull() } ?: return null
        val identity = currentIdToken()?.let { JwtDecoder.decode(it).getOrNull() } ?: return null
        if (access.sub.isBlank() || access.iss.isBlank() ||
            identity.sub != access.sub || identity.iss != access.iss) return null
        return identity.name?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun save(tokens: CognitoTokens) {
        val safe = tokens.copy(refreshToken = null)
        val payload = org.json.JSONObject().put("access", safe.accessToken)
            .put("id", safe.idToken).put("expires", safe.expiresIn ?: 0L).toString()
        val encrypted = vault.encrypt(payload)
        check(prefs.edit().putString("encrypted_tokens", encrypted)
            .remove(KEY_ACCESS).remove(KEY_ID).remove(KEY_REFRESH)
            .putLong(KEY_EXPIRES_IN, safe.expiresIn ?: 0L)
            .putLong(KEY_SAVED_AT, System.currentTimeMillis()).commit())
        _tokens.value = safe
    }

    fun clear() {
        prefs.edit().clear().apply()
        _tokens.value = null
    }

    fun hasValidSession(): Boolean {
        val token = currentAccessToken() ?: return false
        return JwtDecoder.decode(token).map { !it.isExpired && it.sub.isNotBlank() && it.iss.isNotBlank() }.getOrDefault(false)
    }

    fun expiresAtMillis(): Long {
        val savedAt = prefs.getLong(KEY_SAVED_AT, 0L)
        val expiresIn = prefs.getLong(KEY_EXPIRES_IN, 0L)
        return if (savedAt > 0 && expiresIn > 0) savedAt + expiresIn * 1000 else 0L
    }

    private fun readTokens(): CognitoTokens? {
        // Require a new login after upgrading from unencrypted v1.5 storage.
        prefs.edit().remove(KEY_ACCESS).remove(KEY_ID).remove(KEY_REFRESH).apply()
        val encrypted = prefs.getString("encrypted_tokens", null) ?: return null
        return try {
            val json = org.json.JSONObject(vault.decrypt(encrypted))
            CognitoTokens(accessToken = json.getString("access"),
                idToken = if (json.isNull("id")) null else json.getString("id"),
                refreshToken = null, expiresIn = json.getLong("expires"))
        } catch (_: Exception) {
            prefs.edit().clear().apply()
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "syncpass_session"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_ID = "id_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_EXPIRES_IN = "expires_in"
        private const val KEY_SAVED_AT = "saved_at"
    }
}
