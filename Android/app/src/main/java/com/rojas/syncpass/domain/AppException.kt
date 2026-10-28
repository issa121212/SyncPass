package com.rojas.syncpass.domain

enum class ErrorCode { SESION, PERMISO, NO_EXISTE, CONFLICTO, VALIDACION, RED, SERVIDOR, DESCONOCIDO }
class AppException(val codigo: ErrorCode, val reintentable: Boolean, message: String, cause: Throwable? = null) : RuntimeException(message, cause)
