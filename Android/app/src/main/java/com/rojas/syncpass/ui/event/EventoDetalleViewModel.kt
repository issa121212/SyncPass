package com.rojas.syncpass.ui.event

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.rojas.syncpass.domain.model.Evento
import com.rojas.syncpass.domain.model.*
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
    val proyectos: List<Proyecto> = emptyList(),
    val guardando: Boolean = false,
    val eliminado: Boolean = false,
    val revisionGuardado: Int = 0,
    val errorEdicion: String? = null,
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

    val puedeGestionar: Boolean
        get() = evento?.let { e -> proyectos.any { it.id == e.proyectoId && puedeGestionarProyecto(it, perfil) } } == true

    val esDocente: Boolean
        get() = rolesSet.any { it.equals("DOCENTE", true) || it.equals("ADMIN", true) }

    val esVoluntario: Boolean
        get() = rolesSet.any { it.equals("VOLUNTARIO", true) }

    val esParticipante: Boolean
        get() = rolesSet.any { it.equals("PARTICIPANTE", true) }

    val esEstudiante: Boolean
        get() = rolesSet.any { it.equals("ESTUDIANTE", true) }
}

@HiltViewModel
class EventoDetalleViewModel @Inject constructor(
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
    private val repository: SyncPassOperations,
) : ViewModel() {
    private val eventoId: Long = requireNotNull(savedStateHandle.get<Long>("id"))

    private data class Extras(
        val asistentes: List<Asistente> = emptyList(),
        val encuestas: List<EncuestaEvento> = emptyList(),
        val guardando: Boolean = false,
        val eliminado: Boolean = false,
        val revisionGuardado: Int = 0,
        val errorEdicion: String? = null,
        val cargando: Boolean = false,
        val mensaje: String? = null,
        val error: String? = null,
    )

    private var mutando = false
    private val extras = MutableStateFlow(Extras())

    val uiState = combine(
        repository.observarEvento(eventoId),
        repository.perfil,
        repository.proyectos,
        extras,
    ) { evento, perfil, proyectos, extra ->
        EventoDetalleUiState(
            evento = evento,
            perfil = perfil,
            proyectos = proyectos, guardando = extra.guardando, eliminado = extra.eliminado,
            revisionGuardado = extra.revisionGuardado, errorEdicion = extra.errorEdicion,
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
        if (mutando || extras.value.eliminado) return
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

    fun guardarEvento(input: EventoInput) = modificar(false) {
        val actual = repository.observarEvento(eventoId).first() ?: error("Evento no disponible")
        require(input.cupos == 0 || input.cupos >= actual.inscritos) { "El cupo no puede ser menor que los inscritos" }
        repository.editarEvento(eventoId, input).getOrThrow()
    }

    fun cancelarEvento() = modificar(false) {
        val actual = repository.refrescarEvento(eventoId).getOrThrow()
        repository.editarEvento(eventoId, actual.toInput().copy(estado = "CANCELADO")).getOrThrow()
    }

    fun eliminarEvento() = modificar(true) { repository.eliminarEvento(eventoId).getOrThrow() }

    fun limpiarErrorEdicion() { extras.update { it.copy(errorEdicion = null) } }

    private fun modificar(eliminar: Boolean, operation: suspend () -> Any) {
        if (mutando || !uiState.value.puedeGestionar || extras.value.cargando) return
        mutando = true
        extras.update { it.copy(guardando = true, errorEdicion = null) }
        viewModelScope.launch {
            try {
                operation()
                extras.update { it.copy(eliminado = eliminar, revisionGuardado = it.revisionGuardado + 1,
                    mensaje = if (eliminar) null else "Evento guardado en el servidor") }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                extras.update { it.copy(errorEdicion = e.message ?: "No se pudo completar la operación") }
            } finally {
                mutando = false
                extras.update { it.copy(guardando = false) }
            }
        }
    }

    fun limpiarMensaje() {
        extras.update { it.copy(mensaje = null, error = null) }
    }
}
