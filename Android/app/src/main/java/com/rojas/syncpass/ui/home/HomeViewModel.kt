package com.rojas.syncpass.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.domain.model.EventoInput
import com.rojas.syncpass.domain.model.ProyectoInput
import com.rojas.syncpass.domain.SyncPassOperations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SyncPassOperations,
) : ViewModel() {

    private data class Estado(
        val cargando: Boolean = false,
        val creando: Boolean = false,
        val mensaje: String? = null,
        val error: String? = null,
    )

    private val estado = MutableStateFlow(Estado())

    private val datos = combine(
        repository.perfil,
        repository.proyectos,
        repository.eventos,
        repository.pendientes,
    ) { perfil, proyectos, eventos, pendientes ->
        HomeUiState(
            perfil = perfil,
            proyectos = proyectos,
            eventos = eventos,
            pendientes = pendientes,
        )
    }

    val uiState = combine(datos, estado, repository.recintos, repository.rechazadas) { base, est, recintos, rechazadas ->
        base.copy(cargando = est.cargando, creando = est.creando, mensaje = est.mensaje, error = est.error, recintos = recintos, rechazadas = rechazadas)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refrescar()
    }

    fun refrescar() {
        viewModelScope.launch {
            estado.update { it.copy(cargando = true, error = null) }
            repository.refrescar()
                .onFailure { e ->
                    estado.update {
                        it.copy(error = "No se pudo actualizar. Se muestra lo guardado. ${e.message.orEmpty()}")
                    }
                }
            estado.update { it.copy(cargando = false) }
        }
    }

    fun sincronizar() {
        viewModelScope.launch {
            estado.update { it.copy(cargando = true, mensaje = null, error = null) }
            repository.sincronizar()
                .onSuccess { n ->
                    estado.update {
                        it.copy(
                            cargando = false,
                            mensaje = if (n == 0) "Sincronización terminada; revisa pendientes y rechazos" else "$n operaciones sincronizadas",
                        )
                    }
                }
                .onFailure { e ->
                    estado.update {
                        it.copy(cargando = false, error = "Quedan operaciones pendientes de validación o envío. ${e.message.orEmpty()}")
                    }
                }
        }
    }

    fun crearProyecto(
        nombre: String,
        descripcion: String,
        areaTematica: String,
        unidadVecinal: String,
        fechaInicio: String,
        fechaFin: String,
    ) {
        if (estado.value.creando) return
        estado.update { it.copy(creando = true, error = null) }
        viewModelScope.launch {
            try {
                repository.crearProyecto(
                    ProyectoInput(
                        nombre = nombre,
                        descripcion = descripcion.ifBlank { null },
                        areaTematica = areaTematica.ifBlank { null },
                        unidadVecinal = unidadVecinal.ifBlank { null },
                        fechaInicio = fechaInicio.ifBlank { null },
                        fechaFin = fechaFin.ifBlank { null },
                    )
                ).onSuccess {
                    estado.update { it.copy(mensaje = "Proyecto guardado; revisa su estado de sincronización") }
                }.onFailure { e ->
                    estado.update { it.copy(error = "No se pudo crear el proyecto: ${e.message}") }
                }
            } finally { estado.update { it.copy(creando = false) } }
        }
    }

    fun crearEvento(
        proyectoId: Long,
        nombre: String,
        descripcion: String,
        fechaHora: String,
        cupos: Int,
        recinto: String,
        direccion: String,
        latitud: Double?,
        longitud: Double?,
        radioMetros: Int,
    ) {
        if (estado.value.creando) return
        estado.update { it.copy(creando = true, error = null) }
        viewModelScope.launch {
            try {
                repository.crearEvento(
                    EventoInput(
                        proyectoId = proyectoId,
                        nombre = nombre,
                        descripcion = descripcion.ifBlank { null },
                        fechaHora = fechaHora,
                        cupos = cupos,
                        recinto = recinto.ifBlank { null },
                        direccion = direccion.ifBlank { null },
                        latitud = latitud,
                        longitud = longitud,
                        radioMetros = radioMetros,
                    )
                ).onSuccess {
                    estado.update { it.copy(mensaje = "Evento guardado; revisa su estado de sincronización") }
                }.onFailure { e ->
                    estado.update { it.copy(error = "No se pudo crear el evento: ${e.message}") }
                }
            } finally { estado.update { it.copy(creando = false) } }
        }
    }

    fun limpiarMensaje() {
        estado.update { it.copy(mensaje = null, error = null) }
    }
}
