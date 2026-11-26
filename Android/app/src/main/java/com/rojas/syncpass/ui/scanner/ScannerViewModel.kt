package com.rojas.syncpass.ui.scanner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.domain.LocationSource
import com.rojas.syncpass.domain.SyncPassOperations
import com.rojas.syncpass.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

enum class CapturePhase { READING, PREPARING, PERMISSION, LOCATING, SAVING, ERROR, CONFIRMED, PENDING, REJECTED }
data class ScannerState(val phase: CapturePhase = CapturePhase.READING, val message: String? = null)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: SyncPassOperations,
    private val locationSource: LocationSource,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val eventId = requireNotNull(savedStateHandle.get<Long>("id"))
    private val _state = MutableStateFlow(ScannerState())
    val state = _state.asStateFlow()
    private var qr: String? = null
    private var zone: EventZone? = null

    fun preparar(code: String) {
        if (state.value.phase !in setOf(CapturePhase.READING, CapturePhase.ERROR, CapturePhase.REJECTED)) return
        qr = code
        _state.value = ScannerState(CapturePhase.PREPARING, "Preparando captura…")
        viewModelScope.launch {
            attempt {
                require(code.isNotBlank()) { "El QR no contiene datos" }
                val event = requireNotNull(repository.observarEvento(eventId).first()) { "Actualiza el evento antes de escanear" }
                require((event.latitud == null) == (event.longitud == null)) { "La zona del evento está incompleta; consulta al docente" }
                zone = event.latitud?.let { EventZone(GeoPoint(it, requireNotNull(event.longitud)), event.radioMetros).also { z -> z.validate() } }
                if (zone == null) save(null)
                else _state.value = ScannerState(CapturePhase.PERMISSION)
            }
        }
    }

    fun permisoDenegado() {
        if (state.value.phase == CapturePhase.PERMISSION) {
            _state.value = ScannerState(CapturePhase.ERROR, "Permite la ubicación precisa para registrar asistencia en esta zona")
        }
    }

    fun registrarConUbicacion() {
        if (state.value.phase != CapturePhase.PERMISSION) return
        _state.value = ScannerState(CapturePhase.LOCATING, "Obteniendo ubicación reciente…")
        viewModelScope.launch {
            attempt {
                val location = locationSource.currentLocation().getOrThrow()
                ensureActive()
                val capture = AttendanceCapture(clock.instant(), location)
                AttendanceLocationPolicy.validate(requireNotNull(zone), capture)
                save(capture)
            }
        }
    }

    fun reintentar() { qr?.let(::preparar) }
    fun otroQr() {
        if (state.value.phase in setOf(CapturePhase.ERROR, CapturePhase.REJECTED)) {
            qr = null
            _state.value = ScannerState()
        }
    }

    private suspend fun save(capture: AttendanceCapture?) {
        _state.value = ScannerState(CapturePhase.SAVING, "Guardando captura…")
        val result = repository.registrarAsistenciaEscaneo(eventId, requireNotNull(qr), capture ?: AttendanceCapture(clock.instant(), null)).getOrThrow()
        _state.value = when (result) {
            AttendanceOutcome.Confirmed -> ScannerState(CapturePhase.CONFIRMED, "Asistencia confirmada")
            AttendanceOutcome.Pending -> ScannerState(CapturePhase.PENDING, "Captura pendiente de sincronización. Todavía no está confirmada")
            is AttendanceOutcome.Rejected -> ScannerState(CapturePhase.REJECTED, result.reason)
        }
    }

    private suspend fun attempt(block: suspend () -> Unit) {
        try { block() }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { _state.value = ScannerState(CapturePhase.ERROR, error.message ?: "No se pudo guardar la captura") }
    }
}
