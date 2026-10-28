package com.rojas.syncpass.ui.gestion

import androidx.lifecycle.*
import com.rojas.syncpass.domain.model.*
import com.rojas.syncpass.domain.model.ProyectoInput
import com.rojas.syncpass.domain.SyncPassOperations
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GestionViewModel(private val repository: SyncPassOperations) : ViewModel() {
    data class Estado(val guardando: Boolean = false, val mensaje: String? = null, val error: Boolean = false)
    private val _estado = MutableStateFlow(Estado())
    val estado = _estado.asStateFlow()
    val proyectos = repository.proyectos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recintos = repository.recintos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val perfil = repository.perfil.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun ejecutar(operacion: suspend () -> Result<Unit>, mensaje: String, onSuccess: () -> Unit = {}) {
        if (estado.value.guardando) return
        _estado.value = Estado(guardando = true)
        viewModelScope.launch {
            operacion().fold(onSuccess = {
                _estado.value = Estado(mensaje = mensaje); onSuccess()
            }, onFailure = { e -> _estado.value = Estado(mensaje = errorVisible(e), error = true) })
        }
    }
    fun guardarProyecto(id: Long?, request: ProyectoInput, onSuccess: () -> Unit) = ejecutar(
        { if (id == null) repository.crearProyecto(request) else repository.editarProyecto(id, request) },
        "Proyecto guardado en el servidor", onSuccess)
    fun eliminarProyecto(id: Long, onSuccess: () -> Unit) = ejecutar(
        { repository.eliminarProyecto(id) }, "Proyecto eliminado del servidor", onSuccess)
    fun guardarRecinto(item: Recinto, onSuccess: () -> Unit) = ejecutar(
        { repository.guardarRecinto(item) }, "Recinto guardado en este dispositivo", onSuccess)
    fun eliminarRecinto(id: Long, onSuccess: () -> Unit) = ejecutar(
        { repository.eliminarRecinto(id) }, "Recinto eliminado de este dispositivo", onSuccess)
    fun consultarProyecto(id: Long, onSuccess: (Proyecto) -> Unit) {
        ejecutar({ repository.consultarProyecto(id).map { onSuccess(it) } }, "Proyecto consultado en el servidor")
    }
    fun limpiarMensaje() { _estado.update { it.copy(mensaje = null) } }
    private fun errorVisible(e: Throwable): String =
        (e as? com.rojas.syncpass.domain.AppException)?.message ?: "No se pudo completar la operacion"
}
