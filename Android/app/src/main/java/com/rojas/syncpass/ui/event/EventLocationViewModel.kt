package com.rojas.syncpass.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.domain.LocationSource
import com.rojas.syncpass.domain.model.EventZone
import com.rojas.syncpass.domain.model.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import java.time.Clock
import javax.inject.Inject

data class EventLocationState(
    val point: GeoPoint? = null,
    val radius: String = "200",
    val locating: Boolean = false,
    val accuracyMeters: Double? = null,
    val error: String? = null,
)

@HiltViewModel
class EventLocationViewModel @Inject constructor(
    private val locationSource: LocationSource,
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(EventLocationState())
    val state = _state.asStateFlow()
    private var locationJob: Job? = null
    private var requestVersion = 0
    private var dialogId: String? = null

    fun open(id: String, point: GeoPoint?, radius: Int) {
        if (dialogId == id) return
        dialogId = id
        cancelLocation()
        _state.value = EventLocationState(point = point, radius = radius.toString())
    }

    fun selectPoint(point: GeoPoint) {
        cancelLocation()
        runCatching { point.validate() }
            .onSuccess { _state.update { it.copy(point = point, accuracyMeters = null, error = null) } }
            .onFailure { error -> _state.update { it.copy(error = error.message) } }
    }

    fun setRadius(radius: String) {
        _state.update { it.copy(radius = radius, error = null) }
    }

    fun locate() {
        if (locationJob?.isActive == true) return
        val version = requestVersion
        _state.update { it.copy(locating = true, error = null) }
        locationJob = viewModelScope.launch {
            try {
                val result = locationSource.currentLocation()
                ensureActive()
                result.mapCatching { location ->
                    location.validate(clock.millis())
                    location
                }.onSuccess { location ->
                    _state.update { it.copy(point = location.point, accuracyMeters = location.accuracyMeters) }
                }.onFailure { error ->
                    _state.update { it.copy(error = error.message ?: "No se pudo obtener ubicación") }
                }
            } finally {
                if (requestVersion == version) _state.update { it.copy(locating = false) }
            }
        }
    }

    fun permissionDenied() {
        _state.update { it.copy(error = "Permiso de ubicación denegado. Puedes elegir el punto en el mapa") }
    }

    fun confirm(): EventZone? = runCatching {
        EventZone(
            requireNotNull(state.value.point) { "Selecciona el lugar del evento" },
            requireNotNull(state.value.radius.toIntOrNull()) { "Ingresa un radio entero" },
        ).also { it.validate() }
    }.onFailure { error -> _state.update { it.copy(error = error.message) } }.getOrNull()

    fun cancelLocation() {
        requestVersion++
        locationJob?.cancel()
        locationJob = null
        _state.update { it.copy(locating = false) }
    }
}
