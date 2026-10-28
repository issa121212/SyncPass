package com.rojas.syncpass.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.domain.model.Evento
import com.rojas.syncpass.domain.model.Perfil
import com.rojas.syncpass.domain.model.Asistente
import com.rojas.syncpass.domain.model.EncuestaEvento
import com.rojas.syncpass.domain.SyncPassOperations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventoDetalleUiState(
    val evento: Evento? = null,
    val perfil: Perfil? = null,
    val asistentes: List<Asistente> = emptyList(),
    val encuestas: List<EncuestaEvento> = emptyList(),
    val cargando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null,
) {
    private val rolesSet: Set<String>
        get() = perfil?.roles
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()

    val esDocente: Boolean
        get() = rolesSet.any { it.equals("DOCENTE", true) || it.equals("ADMIN", true) }

    val esVoluntario: Boolean
        get() = rolesSet.any { it.equals("VOLUNTARIO", true) }

    val esParticipante: Boolean
        get() = rolesSet.any { it.equals("PARTICIPANTE", true) }

    val esEstudiante: Boolean
        get() = rolesSet.any { it.equals("ESTUDIANTE", true) }
}

class EventoDetalleViewModel(
    private val eventoId: Long,
    private val repository: SyncPassOperations,
) : ViewModel() {

    private data class Extras(
        val asistentes: List<Asistente> = emptyList(),
        val encuestas: List<EncuestaEvento> = emptyList(),
        val cargando: Boolean = false,
        val mensaje: String? = null,
        val error: String? = null,
    )

    private val extras = MutableStateFlow(Extras())

    val uiState = combine(
        repository.observarEvento(eventoId),
        repository.perfil,
        extras,
    ) { evento, perfil, extra ->
        EventoDetalleUiState(
            evento = evento,
            perfil = perfil,
            asistentes = extra.asistentes,
            encuestas = extra.encuestas,
            cargando = extra.cargando,
            mensaje = extra.mensaje,
            error = extra.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventoDetalleUiState())

    init {
        refrescar()
    }

    fun refrescar() {
        viewModelScope.launch {
            extras.update { it.copy(cargando = true, error = null) }
            repository.refrescarEvento(eventoId).onFailure { e ->
                extras.update { it.copy(error = "Sin conexión: se muestra la copia local. ${e.message.orEmpty()}") }
            }
            val perfil = repository.perfil.first()
            val esDocente = perfil?.roles?.split(",")?.any { it.equals("DOCENTE", true) || it.equals("ADMIN", true) } == true
            val esReceptor = esDocente || perfil?.roles?.split(",")?.any { it.equals("VOLUNTARIO", true) || it.equals("ESTUDIANTE", true) } == true
            if (esReceptor) {
                repository.asistentes(eventoId).onSuccess { lista ->
                    extras.update { it.copy(asistentes = lista) }
                }
                if (esDocente) repository.encuestas(eventoId).onSuccess { lista ->
                    extras.update { it.copy(encuestas = lista) }
                }
            }
            extras.update { it.copy(cargando = false) }
        }
    }

    fun inscribirse() {
        viewModelScope.launch {
            val evento = uiState.value.evento ?: return@launch
            extras.update { it.copy(cargando = true, error = null) }
            repository.inscribirse(evento)
                .onSuccess { extras.update { it.copy(mensaje = "Inscripción guardada") } }
                .onFailure { e -> extras.update { it.copy(error = "No se pudo inscribir: ${e.message}") } }
            extras.update { it.copy(cargando = false) }
        }
    }

    fun guardarEncuesta(satisfaccion: Int, utilidad: Int, comentarios: String) {
        viewModelScope.launch {
            extras.update { it.copy(cargando = true, error = null) }
            repository.guardarEncuesta(eventoId, satisfaccion, utilidad, comentarios.ifBlank { null })
                .onSuccess { extras.update { it.copy(mensaje = "Encuesta guardada") } }
                .onFailure { e -> extras.update { it.copy(error = "No se pudo guardar: ${e.message}") } }
            extras.update { it.copy(cargando = false) }
        }
    }

    fun limpiarMensaje() {
        extras.update { it.copy(mensaje = null, error = null) }
    }
}

class EventoDetalleViewModelFactory(
    private val eventoId: Long,
    private val repository: SyncPassOperations,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(EventoDetalleViewModel::class.java))
        return EventoDetalleViewModel(eventoId, repository) as T
    }
}
