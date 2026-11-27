package com.rojas.syncpass

import android.app.Application
import android.content.Context
import android.util.Base64
import com.cognitoauth.CognitoTokens
import com.rojas.syncpass.data.session.SessionStore
import com.rojas.syncpass.data.session.TokenVault
import com.rojas.syncpass.core.jwt.JwtDecoder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Simula únicamente el cifrador; no acredita la seguridad del Keystore físico. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class SessionStoreLocalTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val prefs get() = context.getSharedPreferences("syncpass_session", Context.MODE_PRIVATE)
    private fun jwt(sub: String = "a", issuer: String = "issuer", name: String = "Ana", exp: Long = System.currentTimeMillis()/1000+3600): String {
        val payload = """{"sub":"$sub","iss":"$issuer","name":"$name","exp":$exp,"cognito:groups":["DOCENTE"]}"""
        return "e30."+Base64.encodeToString(payload.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)+".test"
    }
    private fun <T> withVault(block: () -> T): T = mockConstruction(TokenVault::class.java) { vault, _ ->
        `when`(vault.encrypt(anyString())).thenAnswer { "fake:"+Base64.encodeToString(it.getArgument<String>(0).toByteArray(), Base64.NO_WRAP) }
        `when`(vault.decrypt(anyString())).thenAnswer {
            val raw = it.getArgument<String>(0)
            require(raw.startsWith("fake:"))
            String(Base64.decode(raw.removePrefix("fake:"), Base64.NO_WRAP))
        }
    }.use { prefs.edit().clear().commit(); block() }

    @Test fun guardaRestauraSnapshotSinRefreshTokenYLimpiarCierraLaSesion() = withVault {
        val access = jwt()
        val store = SessionStore(context)
        assertFalse(store.hasValidSession())
        assertNull(store.accountKey())
        assertEquals(0L, store.expiresAtMillis())
        store.save(CognitoTokens(accessToken = access, idToken = jwt(name = " Ana "), refreshToken = "no-conservar", expiresIn = 3600))
        assertTrue(store.hasValidSession())
        assertEquals("issuer|a", store.accountKey())
        assertEquals("Ana", store.cognitoName())
        assertEquals(access, store.accessTokenFor("issuer|a"))
        assertNull(store.accessTokenFor("issuer|otra"))
        assertNull(store.tokens.value?.refreshToken)
        assertTrue(store.expiresAtMillis() > System.currentTimeMillis())
        val reopened = SessionStore(context)
        assertEquals(access, reopened.currentAccessToken())
        assertNotNull(reopened.currentIdToken())
        reopened.clear()
        assertNull(reopened.tokens.value)
        assertNull(reopened.accessTokenFor("issuer|a"))
        assertTrue(prefs.all.isEmpty())
    }

    @Test fun tokenExpiradoOIdentidadDeOtraCuentaNoAutorizaNiMezclaNombre() = withVault {
        val store = SessionStore(context)
        store.save(CognitoTokens(accessToken = jwt(exp = 1), idToken = jwt(sub = "otra"), refreshToken = null, expiresIn = 0))
        assertFalse(store.hasValidSession())
        assertNull(store.accessTokenFor("issuer|a"))
        assertNull(store.cognitoName())
        store.save(CognitoTokens(accessToken = jwt(issuer = ""), idToken = null, refreshToken = null, expiresIn = null))
        assertNull(store.accountKey())
        assertFalse(store.hasValidSession())
        assertNull(store.cognitoName())
        store.save(CognitoTokens(accessToken = "malformado", idToken = "malformado", refreshToken = null, expiresIn = null))
        assertFalse(store.hasValidSession())
        assertNull(store.accessTokenFor("issuer|a"))
    }

    @Test fun preferenciasAntiguasOCifradasCorruptasSeEliminanSinExponerTokens() = withVault {
        prefs.edit().putString("access_token", "viejo").putString("refresh_token", "viejo").commit()
        assertNull(SessionStore(context).tokens.value)
        assertFalse(prefs.contains("access_token"))
        assertFalse(prefs.contains("refresh_token"))
        prefs.edit().putString("encrypted_tokens", "corrupto").commit()
        assertNull(SessionStore(context).tokens.value)
        assertTrue(prefs.all.isEmpty())
        assertTrue(JwtDecoder.decode("sin.puntos.extra.invalido").isFailure)
        assertTrue(JwtDecoder.decode("e30.bm8tanNvbg.x").isFailure)
    }
}
