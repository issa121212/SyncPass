package com.rojas.syncpass

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import com.rojas.syncpass.domain.LocationSource
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.ui.event.EventLocationDialog
import com.rojas.syncpass.ui.event.EventLocationViewModel
import com.rojas.syncpass.ui.theme.SyncPassTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Clock

class EventLocationDialogUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun errorDeRadioSeMuestraCompletoSinScrollYPermiteCorregirLaZona() {
        val source = object : LocationSource {
            override suspend fun currentLocation(): Result<DeviceLocation> = error("Esta prueba no solicita ubicación ni permisos")
        }
        val vm = EventLocationViewModel(source, Clock.systemUTC())
        val store = ViewModelStore().apply { put("location", vm) }
        val point = GeoPoint(-33.45, -70.66)
        var confirmed: EventZone? = null
        try {
            compose.setContent {
                SyncPassTheme { EventLocationDialog("visible-error", point, 200, {}, { confirmed = it }, vm) }
            }
            compose.onNodeWithText("Radio permitido (m)").performTextReplacement("0")
            compose.onNodeWithText("Usar esta zona").performClick()
            val error = compose.onNodeWithText("El radio debe estar entre 1 y 50000 metros").assertIsDisplayed().fetchSemanticsNode()
            assertEquals("El mensaje debe estar completo, no solo una franja visible",
                error.layoutInfo.coordinates.size.height.toFloat(), error.boundsInRoot.height, 1f)
            assertNull(confirmed)
            compose.onNodeWithText("Radio permitido (m)").performScrollTo().performTextReplacement("250")
            compose.onNodeWithText("Usar esta zona").performClick()
            compose.runOnIdle { assertEquals(EventZone(point, 250), confirmed) }
        } finally { store.clear() }
    }
}
