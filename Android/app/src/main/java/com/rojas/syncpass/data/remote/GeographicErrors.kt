package com.rojas.syncpass.data.remote

import com.google.gson.JsonParser
import retrofit2.HttpException

/** Solo traduce códigos conocidos del servidor; no presenta cuerpos HTTP arbitrarios. */
internal fun HttpException.geographicMessage(): String? = runCatching {
    val body = response()?.errorBody()?.string()?.take(4096) ?: return@runCatching null
    val code = JsonParser.parseString(body).asJsonObject.get("mensaje")?.asString
    when (code) {
        "GEO_ZONA_INVALIDA" -> "La zona del evento está incompleta; consulta al docente"
        "GEO_DATOS_AUSENTES" -> "Faltan datos de ubicación; realiza una nueva captura con la versión actualizada"
        "GEO_COORDENADAS_INVALIDAS" -> "Las coordenadas de la captura no son válidas"
        "GEO_PRECISION_INSUFICIENTE" -> "La ubicación es imprecisa; activa la ubicación precisa y vuelve a escanear"
        "GEO_UBICACION_ANTIGUA" -> "La ubicación guardada es antigua; realiza una nueva captura"
        "GEO_CAPTURA_FUERA_DE_PLAZO" -> "La captura excedió el plazo de 24 horas o tiene una hora incorrecta"
        "GEO_FUERA_DE_ZONA" -> "La captura se realizó fuera de la zona permitida"
        else -> null
    }
}.getOrNull()
