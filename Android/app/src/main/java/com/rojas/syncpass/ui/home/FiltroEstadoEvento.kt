package com.rojas.syncpass.ui.home

import com.rojas.syncpass.domain.model.Evento

enum class FiltroEstadoEvento(val etiqueta: String, private val estado: String? = null) {
    TODOS("Todos"),
    PROGRAMADO("Programado", "PROGRAMADO"),
    EN_CURSO("En curso", "EN_CURSO"),
    FINALIZADO("Finalizado", "FINALIZADO"),
    CANCELADO("Cancelado", "CANCELADO");

    fun aplicar(eventos: List<Evento>): List<Evento> =
        if (estado == null) eventos else eventos.filter { it.estado == estado }
}
