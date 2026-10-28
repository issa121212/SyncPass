package com.rojas.syncpass.ui.scanner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.domain.SyncPassOperations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
class ScannerViewModel(private val repository: SyncPassOperations) : ViewModel() {
    private val _resultado = MutableStateFlow<String?>(null)
    val resultado = _resultado.asStateFlow()
    fun registrar(id: Long, qr: String, lat: Double?, lon: Double?, precision: Double?) {
        viewModelScope.launch {
            _resultado.value = repository.registrarAsistenciaEscaneo(id, qr, lat, lon, precision).fold(
                onSuccess = { if (it) "Asistencia confirmada" else "Captura pendiente. Revisa sincronizacion y operaciones rechazadas." },
                onFailure = { it.message ?: "No se pudo guardar la captura" })
        }
    }
}
