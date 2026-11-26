package com.rojas.syncpass

import androidx.lifecycle.ViewModelStore
import com.rojas.syncpass.domain.LocationSource
import com.rojas.syncpass.domain.model.DeviceLocation
import com.rojas.syncpass.domain.model.EventZone
import com.rojas.syncpass.domain.model.GeoPoint
import com.rojas.syncpass.ui.event.EventLocationViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class EventLocationViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val now = 1_000_000L
    private val clock = Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC)
    private val store = ViewModelStore()
    private val selected = GeoPoint(-33.45, -70.66)

    private class FakeLocationSource : LocationSource {
        var calls = 0
        val response = CompletableDeferred<Result<DeviceLocation>>()
        override suspend fun currentLocation(): Result<DeviceLocation> {
            calls++
            return response.await()
        }
    }

    private val source = FakeLocationSource()
    private fun viewModel() = EventLocationViewModel(source, clock).also { store.put("location", it) }

    @After fun release() { store.clear() }

    @Test fun confirmaLaZonaExistenteYExigeUnPuntoEnEventosAntiguosSinUbicacion() {
        val vm = viewModel()
        vm.open("existing", selected, 150)
        assertEquals(EventZone(selected, 150), vm.confirm())
        vm.open("legacy", null, 200)
        assertNull(vm.confirm())
        assertNotNull(vm.state.value.error)
        vm.selectPoint(selected)
        assertEquals(EventZone(selected, 200), vm.confirm())
    }

    @Test fun radioInvalidoBloqueaLaConfirmacionYSePuedeCorregir() {
        val vm = viewModel()
        vm.open("zone", selected, 200)
        for (invalid in listOf("", "0", "-5", "2.5", "50001")) {
            vm.setRadius(invalid)
            assertNull(vm.confirm())
            assertNotNull(vm.state.value.error)
        }
        vm.setRadius("350")
        assertEquals(EventZone(selected, 350), vm.confirm())
        assertNull(vm.state.value.error)
    }

    @Test fun resultadoDelDispositivoActualizaElCentroUnaSolaVez() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.open("zone", null, 200)
        vm.locate()
        vm.locate()
        runCurrent()
        assertEquals(1, source.calls)
        assertTrue(vm.state.value.locating)
        source.response.complete(Result.success(DeviceLocation(selected, 12.0, now - 1000)))
        advanceUntilIdle()
        assertEquals(selected, vm.state.value.point)
        assertEquals(12.0, vm.state.value.accuracyMeters)
        assertFalse(vm.state.value.locating)
        assertNull(vm.state.value.error)
    }

    @Test fun ubicacionAntiguaNoReemplazaLaZonaGuardada() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.open("zone", selected, 200)
        source.response.complete(Result.success(DeviceLocation(GeoPoint(0.0, 0.0), 12.0, now - 120_001)))
        vm.locate()
        advanceUntilIdle()
        assertEquals(selected, vm.state.value.point)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.locating)
    }

    @Test fun seleccionarManualmenteCancelaUnaSolicitudQueTodaviaEstaPendiente() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.open("zone", null, 200)
        vm.locate()
        runCurrent()
        vm.selectPoint(selected)
        source.response.complete(Result.success(DeviceLocation(GeoPoint(0.0, 0.0), 10.0, now)))
        advanceUntilIdle()
        assertEquals(selected, vm.state.value.point)
        assertNull(vm.state.value.accuracyMeters)
        assertFalse(vm.state.value.locating)
    }

    @Test fun permisoDenegadoPermiteSeguirConLaSeleccionManual() {
        val vm = viewModel()
        vm.open("zone", null, 200)
        vm.permissionDenied()
        assertNotNull(vm.state.value.error)
        vm.selectPoint(selected)
        assertEquals(EventZone(selected, 200), vm.confirm())
        assertEquals(0, source.calls)
    }

    @Test fun recrearElDialogoConservaLosCambiosYUnaAperturaNuevaRecargaElOriginal() {
        val vm = viewModel()
        vm.open("first", null, 200)
        vm.selectPoint(selected)
        vm.setRadius("400")
        vm.open("first", null, 200)
        assertEquals(EventZone(selected, 400), vm.confirm())
        vm.open("second", GeoPoint(1.0, 2.0), 300)
        assertEquals(EventZone(GeoPoint(1.0, 2.0), 300), vm.confirm())
    }

    @Test fun falloDeUbicacionConservaLaZonaYPermiteReintentar() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.open("zone", selected, 200)
        source.response.complete(Result.failure(IllegalArgumentException("Activa la ubicación")))
        vm.locate()
        advanceUntilIdle()
        assertEquals(selected, vm.state.value.point)
        assertEquals("Activa la ubicación", vm.state.value.error)
        assertFalse(vm.state.value.locating)
    }
}
