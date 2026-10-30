package com.rojas.syncpass.domain.model

import java.time.LocalDateTime

val estadosEvento = listOf("PROGRAMADO", "EN_CURSO", "FINALIZADO", "CANCELADO")

fun EventoInput.validar() {
    require(proyectoId > 0) { "Selecciona un proyecto" }
    require(nombre.isNotBlank() && nombre.trim().length <= 160) { "Nombre obligatorio, máximo 160 caracteres" }
    require((descripcion?.length ?: 0) <= 4000) { "Descripción: máximo 4000 caracteres" }
    require(runCatching { LocalDateTime.parse(fechaHora) }.isSuccess) { "Fecha y hora inválidas" }
    require(cupos >= 0) { "Los cupos deben ser un entero mayor o igual a cero" }
    require((recinto?.length ?: 0) <= 160) { "Recinto: máximo 160 caracteres" }
    require((direccion?.length ?: 0) <= 240) { "Dirección: máximo 240 caracteres" }
    require((latitud == null) == (longitud == null)) { "Completa ambas coordenadas" }
    require(latitud == null || (latitud.isFinite() && latitud in -90.0..90.0)) { "Latitud inválida" }
    require(longitud == null || (longitud.isFinite() && longitud in -180.0..180.0)) { "Longitud inválida" }
    require(radioMetros in 1..50000) { "Radio: entre 1 y 50000 metros" }
    require(estado in estadosEvento) { "Estado inválido" }
}

fun Evento.toInput() = EventoInput(proyectoId, nombre, descripcion, fechaHora, cupos,
    recinto, direccion, latitud, longitud, radioMetros, estado)

fun puedeGestionarProyecto(proyecto: Proyecto, perfil: Perfil?): Boolean {
    val roles = perfil?.roles?.split(",")?.map { it.trim().uppercase() }.orEmpty()
    return "ADMIN" in roles || ("DOCENTE" in roles && proyecto.responsableId == perfil?.usuarioId)
}
