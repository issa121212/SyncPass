package com.rojas.syncpass

import com.rojas.syncpass.data.location.firstValidLocation
import com.rojas.syncpass.domain.model.DeviceLocation
import com.rojas.syncpass.domain.model.GeoPoint
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationCandidatesTest {
    private val clock = Clock.fixed(Instant.ofEpochMilli(1_000_000), ZoneOffset.UTC)
    private val valid = DeviceLocation(GeoPoint(-33.45, -70.66), 40.0, clock.millis())

    @Test fun redRespondeSinGpsYCancelaLaSolicitudRestante() = runTest {
        var cancelled = false
        val result = firstValidLocation(listOf("gps", "network"), clock) {
            if (it == "gps") {
                try { awaitCancellation() } finally { cancelled = true }
            } else { delay(100); valid }
        }
        assertEquals(valid, result)
        assertTrue(cancelled)
    }

    @Test fun gpsRespondeAunqueLaRedFalle() = runTest {
        assertEquals(valid, firstValidLocation(listOf("gps", "network"), clock) {
            if (it == "network") error("Sin red") else { delay(100); valid }
        })
    }

    @Test fun descartaPosicionesAntiguasOSinPrecisionYEsperaOtraFuente() = runTest {
        for (invalid in listOf(valid.copy(recordedAtMillis = 1), valid.copy(accuracyMeters = Double.NaN))) {
            assertEquals(valid, firstValidLocation(listOf("gps", "network"), clock) {
                if (it == "gps") invalid else { delay(100); valid }
            })
        }
    }

    @Test fun tiempoLimiteCancelaTodasLasFuentes() = runTest {
        var cancelled = 0
        assertNull(firstValidLocation(listOf("gps", "network"), clock, 500) {
            try { awaitCancellation() } finally { cancelled++ }
        })
        assertEquals(2, cancelled)
    }

    @Test fun cancelacionDePantallaSePropagaALasFuentes() = runTest {
        var cancelled = 0
        val job = launch {
            firstValidLocation(listOf("gps", "network"), clock) {
                try { awaitCancellation() } finally { cancelled++ }
            }
            fail("No debe completar tras cancelarse")
        }
        runCurrent()
        job.cancel()
        job.join()
        assertEquals(2, cancelled)
    }

    @Test fun respuestasNulasTerminanSinEsperarElLimite() = runTest {
        assertNull(firstValidLocation(listOf("gps", "network"), clock) { null })
        assertEquals(0L, testScheduler.currentTime)
    }
}
