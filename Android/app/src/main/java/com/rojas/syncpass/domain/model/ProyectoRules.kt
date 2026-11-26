package com.rojas.syncpass.domain.model

import java.time.LocalDate

fun ProyectoInput.validar() {
    require(nombre.isNotBlank() && nombre.trim().length <= 160) { "Nombre obligatorio, máximo 160 caracteres" }
    require((descripcion?.length ?: 0) <= 4000) { "Descripción: máximo 4000 caracteres" }
    require((areaTematica?.length ?: 0) <= 80) { "Área temática: máximo 80 caracteres" }
    require((unidadVecinal?.length ?: 0) <= 160) { "Unidad vecinal: máximo 160 caracteres" }
    val start = fechaInicio?.let { runCatching { LocalDate.parse(it) }.getOrElse { throw IllegalArgumentException("Fecha de inicio inválida") } }
    val end = fechaFin?.let { runCatching { LocalDate.parse(it) }.getOrElse { throw IllegalArgumentException("Fecha de fin inválida") } }
    require(start == null || end == null || !end.isBefore(start)) { "La fecha de fin no puede ser anterior al inicio" }
    require(estado in listOf("PLANIFICADO", "EN_CURSO", "FINALIZADO", "CANCELADO")) { "Estado de proyecto inválido" }
}
