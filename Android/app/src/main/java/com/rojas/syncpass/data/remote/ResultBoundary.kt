package com.rojas.syncpass.data.remote

import com.rojas.syncpass.domain.*
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

fun Throwable.toAppError(): AppException = when (this) {
    is AppException -> this
    is SessionInvalidIOException -> AppException(ErrorCode.SESION, false, "Inicia sesion nuevamente", this)
    is HttpException -> when (code()) {
        401 -> AppException(ErrorCode.SESION, false, "Inicia sesion nuevamente", this)
        403 -> AppException(ErrorCode.PERMISO, false, "No tienes permisos para esta operacion", this)
        404 -> AppException(ErrorCode.NO_EXISTE, false, "El registro no existe; actualiza el listado", this)
        409 -> AppException(ErrorCode.CONFLICTO, false, "La operacion entra en conflicto con datos o historial existentes", this)
        408, 429 -> AppException(ErrorCode.RED, true, "Servicio temporalmente no disponible", this)
        in 500..599 -> AppException(ErrorCode.SERVIDOR, true, "Servicio temporalmente no disponible", this)
        else -> AppException(ErrorCode.VALIDACION, false, "El servidor rechazo los datos. Revisa la operacion", this)
    }
    is java.io.IOException -> AppException(ErrorCode.RED, true, "No se pudo conectar con el servidor", this)
    is IllegalArgumentException -> AppException(ErrorCode.VALIDACION, false, message ?: "Datos invalidos", this)
    else -> AppException(ErrorCode.DESCONOCIDO, false, "No se pudo completar la operacion", this)
}
suspend fun <T> resultado(block: suspend () -> T): Result<T> = try { Result.success(block()) }
catch (e: CancellationException) { throw e }
catch (e: Exception) { Result.failure(e.toAppError()) }
