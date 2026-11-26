package com.rojas.syncpass.domain.model

/** Nunca usa el ID interno como sustituto de un código que todavía no llegó. */
fun codigoPublicoVisible(codigo: String?, syncState: String): String = when {
    !codigo.isNullOrBlank() -> codigo
    syncState == "PENDIENTE" -> "Código pendiente de sincronización"
    syncState == "RECHAZADO" -> "Código no asignado"
    else -> "Código no disponible"
}
