package com.rojas.syncpass.ui.home

import com.rojas.syncpass.domain.model.Evento
import com.rojas.syncpass.domain.model.Perfil
import com.rojas.syncpass.domain.model.Proyecto

data class HomeUiState(
    val perfil: Perfil? = null,
    val proyectos: List<Proyecto> = emptyList(),
    val eventos: List<Evento> = emptyList(),
    val pendientes: Int = 0,
    val recintos: List<com.rojas.syncpass.domain.model.Recinto> = emptyList(),
    val rechazadas: List<com.rojas.syncpass.domain.OperacionRechazada> = emptyList(),
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
