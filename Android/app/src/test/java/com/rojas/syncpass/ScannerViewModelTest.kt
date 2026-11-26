package com.rojas.syncpass

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.rojas.syncpass.domain.LocationSource
import com.rojas.syncpass.domain.SyncPassOperations
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.ui.scanner.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import java.time.*

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val instant = Instant.parse("2026-11-25T12:00:00Z")
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)
    private val point = GeoPoint(-33.45, -70.66)
    private val store = ViewModelStore()
    private class FakeRepository : SyncPassOperations by mock(SyncPassOperations::class.java) {
        val event = MutableStateFlow<Evento?>(null)
        val captures = mutableListOf<AttendanceCapture>()
        var result: Result<AttendanceOutcome> = Result.success(AttendanceOutcome.Confirmed)
        override fun observarEvento(id: Long) = event
        override suspend fun registrarAsistenciaEscaneo(eventoId: Long, codigoQr: String, captura: AttendanceCapture): Result<AttendanceOutcome> {
            check(eventoId == 7L && codigoQr == "PARTICIPANT")
            captures += captura
            return result
        }
    }
    private val repo = FakeRepository()
    private var locationCalls = 0
    private var response = CompletableDeferred<Result<DeviceLocation>>()
    private val source = object : LocationSource {
        override suspend fun currentLocation(): Result<DeviceLocation> { locationCalls++; return response.await() }
    }
    private fun event(withZone: Boolean = true) = Evento(7, 1, "Proyecto", "Evento", null, "2026-11-25T12:00:00", 20, 0, 0,
        null, null, if (withZone) point.latitude else null, if (withZone) point.longitude else null,
        200, null, "PROGRAMADO", false, false, false, null)
    private fun vm() = ScannerViewModel(repo, source, clock, SavedStateHandle(mapOf("id" to 7L))).also { store.put("scanner", it) }
    @After fun release() { store.clear() }

    @Test fun capturaExigePermisoYEvitaLecturasYEnviosDuplicados() = runTest(main.dispatcher) {
        repo.event.value = event()
        val vm = vm()
        vm.preparar("PARTICIPANT"); vm.preparar("PARTICIPANT"); runCurrent()
        assertEquals(CapturePhase.PERMISSION, vm.state.value.phase)
        assertTrue(repo.captures.isEmpty())
        vm.registrarConUbicacion(); vm.registrarConUbicacion(); runCurrent()
        assertEquals(1, locationCalls)
        response.complete(Result.success(DeviceLocation(point, 10.0, instant.toEpochMilli() - 1500)))
        advanceUntilIdle()
        assertEquals(CapturePhase.CONFIRMED, vm.state.value.phase)
        assertEquals(listOf(AttendanceCapture(instant, DeviceLocation(point, 10.0, instant.toEpochMilli() - 1500))), repo.captures)
    }

    @Test fun permisoDenegadoNoGuardaYReintentaElMismoQr() = runTest(main.dispatcher) {
        repo.event.value = event()
        val vm = vm()
        vm.preparar("PARTICIPANT"); runCurrent(); vm.permisoDenegado()
        assertEquals(CapturePhase.ERROR, vm.state.value.phase)
        assertTrue(repo.captures.isEmpty())
        vm.reintentar(); runCurrent()
        assertEquals(CapturePhase.PERMISSION, vm.state.value.phase)
        response.complete(Result.success(DeviceLocation(point, 10.0, instant.toEpochMilli())))
        vm.registrarConUbicacion(); advanceUntilIdle()
        assertEquals(1, repo.captures.size)
    }

    @Test fun errorDelProveedorPermiteReintentarYUbicacionFueraDeZonaNoSeEncola() = runTest(main.dispatcher) {
        repo.event.value = event()
        val vm = vm()
        response.complete(Result.failure(IllegalArgumentException("Activa la ubicación")))
        vm.preparar("PARTICIPANT"); runCurrent(); vm.registrarConUbicacion(); advanceUntilIdle()
        assertEquals("Activa la ubicación", vm.state.value.message)
        response = CompletableDeferred(Result.success(DeviceLocation(GeoPoint(0.0, 0.0), 10.0, instant.toEpochMilli())))
        vm.reintentar(); runCurrent(); vm.registrarConUbicacion(); advanceUntilIdle()
        assertEquals(CapturePhase.ERROR, vm.state.value.phase)
        assertTrue(repo.captures.isEmpty())
    }

    @Test fun eventoHistoricoSinZonaNoPideSensorYDistinguePendienteDeRechazado() = runTest(main.dispatcher) {
        repo.event.value = event(false)
        repo.result = Result.success(AttendanceOutcome.Pending)
        val vm = vm()
        vm.preparar("PARTICIPANT"); advanceUntilIdle()
        assertEquals(CapturePhase.PENDING, vm.state.value.phase)
        assertEquals(0, locationCalls)
        assertNull(repo.captures.single().location)
        store.clear()
        repo.result = Result.success(AttendanceOutcome.Rejected("Fuera de zona"))
        val other = vm()
        other.preparar("PARTICIPANT"); advanceUntilIdle()
        assertEquals(CapturePhase.REJECTED, other.state.value.phase)
        assertEquals("Fuera de zona", other.state.value.message)
    }

    @Test fun cerrarPantallaCancelaLaBusquedaSinEncolarUnaCaptura() = runTest(main.dispatcher) {
        repo.event.value = event()
        val vm = vm()
        vm.preparar("PARTICIPANT"); runCurrent(); vm.registrarConUbicacion(); runCurrent()
        store.clear()
        response.complete(Result.success(DeviceLocation(point, 10.0, instant.toEpochMilli())))
        advanceUntilIdle()
        assertTrue(repo.captures.isEmpty())
    }

    @Test fun eventoAusenteNoEsTratadoComoEventoSinZona() = runTest(main.dispatcher) {
        val vm = vm()
        vm.preparar("PARTICIPANT"); advanceUntilIdle()
        assertEquals(CapturePhase.ERROR, vm.state.value.phase)
        assertEquals(0, locationCalls)
        assertTrue(repo.captures.isEmpty())
    }
}
