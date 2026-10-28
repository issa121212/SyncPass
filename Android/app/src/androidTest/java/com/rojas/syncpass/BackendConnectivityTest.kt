package com.rojas.syncpass

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

@RunWith(AndroidJUnit4::class)
class BackendConnectivityTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun appShowsCognitoLoginOnFreshLaunch() {
        compose.onNodeWithText("Continuar con Cognito").assertIsDisplayed()
    }

    @Test fun emulatorCanReachBackendHealth() {
        val connection = URL(BuildConfig.API_BASE_URL + "api/health").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            assertEquals(200, connection.responseCode)
            assertTrue(connection.inputStream.bufferedReader().use { it.readText() }.contains("UP"))
        } finally { connection.disconnect() }
    }
}
