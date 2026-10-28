package com.rojas.syncpass

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cognitoauth.CognitoTokens
import com.rojas.syncpass.data.session.SessionStore
import org.junit.Assert.*
import org.junit.Test

class SessionSecurityTest {
    @Test fun tokensAreEncryptedAndSurviveReopeningButRefreshIsDiscarded() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SessionStore(context)
        store.clear()
        try {
            store.save(CognitoTokens(accessToken = "private-access-token", idToken = "private-id-token",
                refreshToken = "private-refresh-token", expiresIn = 3600L))
            val prefs = context.getSharedPreferences("syncpass_session", Context.MODE_PRIVATE)
            assertFalse(prefs.all.toString().contains("private-"))
            val reopened = SessionStore(context)
            assertEquals("private-access-token", reopened.currentAccessToken())
            assertEquals("private-id-token", reopened.currentIdToken())
            assertNull(reopened.tokens.value?.refreshToken)
            reopened.clear()
            assertNull(SessionStore(context).currentAccessToken())
        } finally { store.clear() }
    }
    @Test fun upgradeRemovesLegacyPlaintextAndRequiresLogin() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("syncpass_session", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("access_token","old-access").putString("id_token","old-id")
            .putString("refresh_token","old-refresh").commit()
        assertNull(SessionStore(context).currentAccessToken())
        assertFalse(prefs.contains("access_token"))
        assertFalse(prefs.contains("refresh_token"))
    }
}
